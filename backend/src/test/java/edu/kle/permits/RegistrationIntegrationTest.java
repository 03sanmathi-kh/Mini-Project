package edu.kle.permits;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Two-credential registration: registration credentials only check status; Admin-created credentials log in. */
@SpringBootTest(properties={
 "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000}",
 "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}","spring.datasource.username=${TEST_DB_USER:sa}","spring.datasource.password=${TEST_DB_PASSWORD:}",
 "app.jwt-secret=test-secret-with-at-least-thirty-two-bytes-long","app.admin-password=InitialAdminPassword!","app.admin-local-part=admin"})
@AutoConfigureMockMvc class RegistrationIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired RegistrationService service;@Autowired RegistrationRequests requests;@Autowired Users users;@Autowired PasswordEncoder encoder;@Autowired JdbcTemplate jdbc;
 @MockitoBean Clock clock;
 // Real current time: JWT expiry is validated against the system clock.
 final Instant now=Instant.now().truncatedTo(ChronoUnit.SECONDS);static final AtomicInteger address=new AtomicInteger();static final AtomicInteger seq=new AtomicInteger();
 static final String REG_PASSWORD="RegisterMe#2026",LOGIN_PASSWORD="LoginPass#9876",MEMBER_PASSWORD="ExistingMember#1";AppUser admin,member;
 @BeforeEach void setup(){when(clock.instant()).thenReturn(now);admin=users.findByEmail("admin@permitworks.com").orElseThrow();admin.enabled=true;admin.activationPending=false;admin.locked=false;admin.failedAttempts=0;users.save(admin);member=users.findByEmail("registration-existing@permitworks.com").orElseGet(()->users.save(new AppUser("Existing Member","registration-existing@permitworks.com","existing-registration@example.test",encoder.encode(MEMBER_PASSWORD),Role.APPLICANT)));member.enabled=true;member.activationPending=false;member.locked=false;member.failedAttempts=0;member.role=Role.APPLICANT;users.save(member);}
 String email(){return "applicant-"+UUID.randomUUID()+"@example.test";}
 String loginEmail(){return "pw-user-"+seq.incrementAndGet()+"-"+UUID.randomUUID().toString().substring(0,8)+"@permitworks.com";}
 String ip(){int n=address.incrementAndGet();return "10.72."+(n/250)+"."+(n%250+1);}
 Map<String,Object> input(String email,String role){return new HashMap<>(Map.of("fullName","Alex Example","personalEmail",email,"registrationPassword",REG_PASSWORD,"confirmRegistrationPassword",REG_PASSWORD,"phone","9876543210","requestedRole",role,"description","Needs applicant access"));}
 MockHttpServletRequestBuilder postJson(String path,Object body,String source)throws Exception{return post(path).with(csrf()).with(r->{r.setRemoteAddr(source);return r;}).contentType("application/json").content(json.writeValueAsString(body));}
 Long submit(String email,String role)throws Exception{mvc.perform(postJson("/api/registrations",input(email,role),ip())).andExpect(status().isCreated()).andExpect(jsonPath("$.trackingCode").doesNotExist()).andExpect(jsonPath("$.activationSecret").doesNotExist());return requests.findByPersonalEmailOrderBySubmittedAtDesc(email.trim().toLowerCase(Locale.ROOT)).get(0).id;}
 Cookie login(String email,String password)throws Exception{return mvc.perform(postJson("/api/auth/login",Map.of("email",email,"password",password),ip())).andExpect(status().isOk()).andReturn().getResponse().getCookie("PERMIT_TOKEN");}
 Cookie adminToken()throws Exception{return login(admin.email,"InitialAdminPassword!");}
 Map<String,Object> approval(String role,String login,String password){return new HashMap<>(Map.of("approvedRole",role,"loginEmail",login,"loginPassword",password,"confirmLoginPassword",password,"confirm",true,"adminRemarks","Approved verification remarks"));}
 ResultActions checkStatus(String email,String password)throws Exception{return mvc.perform(postJson("/api/registrations/status",Map.of("personalEmail",email,"registrationPassword",password),ip()));}

 @Test void T01_T02_T07_validRegistrationWithTenDigitPhoneIsPendingWithoutAccount()throws Exception{
  long before=users.count();String e=email();Long id=submit(e,"APPLICANT");RegistrationRequest r=requests.findById(id).orElseThrow();
  assertEquals(RegistrationStatus.PENDING,r.status);assertEquals("9876543210",r.phone);assertNull(r.userId);assertNull(r.trackingHash);assertEquals(before,users.count());
 }
 @Test void T03_invalidPhonesRejected()throws Exception{
  for(String phone:List.of("12345","98765432101","98765abcde","+919876543210","987 654 3210","")){var d=input(email(),"APPLICANT");d.put("phone",phone);mvc.perform(postJson("/api/registrations",d,ip())).andExpect(status().isBadRequest());}
 }
 @Test void T04_adminAbsentFromPublicRoleList()throws Exception{
  String body=mvc.perform(get("/api/registrations/roles")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(body.contains("\"ADMIN\""));assertTrue(body.contains("\"APPLICANT\""));
 }
 @Test void T05_adminAndUnknownRolesRejectedByBackend()throws Exception{
  long before=requests.count();mvc.perform(postJson("/api/registrations",input(email(),"ADMIN"),ip())).andExpect(status().isBadRequest());mvc.perform(postJson("/api/registrations",input(email(),"SUPER_ADMIN"),ip())).andExpect(status().isBadRequest());assertEquals(before,requests.count());
 }
 @Test void T06_registrationPasswordStoredAsBCryptOnly()throws Exception{
  Long id=submit(email(),"APPLICANT");String hash=requests.findById(id).orElseThrow().registrationPasswordHash;assertTrue(hash.startsWith("$2"));assertNotEquals(REG_PASSWORD,hash);assertTrue(encoder.matches(REG_PASSWORD,hash));
 }
 @Test void weakOrMismatchedRegistrationPasswordRejected()throws Exception{
  var weak=input(email(),"APPLICANT");weak.put("registrationPassword","alllowercase");weak.put("confirmRegistrationPassword","alllowercase");mvc.perform(postJson("/api/registrations",weak,ip())).andExpect(status().isBadRequest());
  var mismatch=input(email(),"APPLICANT");mismatch.put("confirmRegistrationPassword","Different#2026");mvc.perform(postJson("/api/registrations",mismatch,ip())).andExpect(status().isBadRequest());
  var missing=input(email(),"APPLICANT");missing.remove("confirmRegistrationPassword");mvc.perform(postJson("/api/registrations",missing,ip())).andExpect(status().isBadRequest());
 }
 @Test void T08_T09_statusCheckUsesPersonalEmailAndRegistrationPasswordWithGenericErrors()throws Exception{
  String e=email();submit(e,"APPLICANT");
  checkStatus(e,REG_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.message").value("Registration awaiting Admin approval.")).andExpect(jsonPath("$.loginEmail").doesNotExist()).andExpect(jsonPath("$.id").doesNotExist());
  String wrong=checkStatus(e,"WrongPass#2026").andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
  String unknown=checkStatus(email(),REG_PASSWORD).andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
  assertEquals(wrong,unknown);
  mvc.perform(postJson("/api/registrations/status",Map.of("personalEmail",e,"trackingCode","PWREG-x"),ip())).andExpect(status().isBadRequest());
 }
 @Test void T10_registrationCredentialsCannotLogIn()throws Exception{
  String e=email();submit(e,"APPLICANT");
  int code=mvc.perform(postJson("/api/auth/login",Map.of("email",e,"password",REG_PASSWORD),ip())).andReturn().getResponse().getStatus();assertTrue(code==400||code==401);
  String pwLooking="reg-only-"+UUID.randomUUID().toString().substring(0,8)+"@permitworks.com";submit(pwLooking,"APPLICANT");
  mvc.perform(postJson("/api/auth/login",Map.of("email",pwLooking,"password",REG_PASSWORD),ip())).andExpect(status().isUnauthorized()).andExpect(cookie().doesNotExist("PERMIT_TOKEN"));
 }
 @Test void T11_nonAdminApprovalDenied()throws Exception{
  Long id=submit(email(),"APPLICANT");Cookie token=login(member.email,MEMBER_PASSWORD);
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",loginEmail(),LOGIN_PASSWORD),ip()).cookie(token)).andExpect(status().isForbidden());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/reject",Map.of("reason","no","confirm",true),ip()).cookie(token)).andExpect(status().isForbidden());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",loginEmail(),LOGIN_PASSWORD),ip())).andExpect(status().isUnauthorized());
  assertEquals(RegistrationStatus.PENDING,requests.findById(id).orElseThrow().status);
 }
 @Test void T12_to_T20_fullApprovalFlowWithSeparateCredentials()throws Exception{
  String personal=email(),login=loginEmail();Long id=submit(personal,"PERMIT_REVIEWER");long before=users.count();Cookie adminCookie=adminToken();
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("PERMIT_REVIEWER",login,LOGIN_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.loginEmail").value(login)).andExpect(jsonPath("$.registrationPasswordHash").doesNotExist());
  RegistrationRequest r=requests.findById(id).orElseThrow();assertEquals(RegistrationStatus.APPROVED,r.status);assertNotNull(r.userId);assertEquals(before+1,users.count());
  AppUser u=users.findById(r.userId).orElseThrow();assertEquals(login,u.email);assertEquals(Role.PERMIT_REVIEWER,u.role);assertTrue(u.enabled);assertFalse(u.activationPending);assertNull(u.personalContactEmail);
  assertTrue(u.passwordHash.startsWith("$2"));assertTrue(encoder.matches(LOGIN_PASSWORD,u.passwordHash));assertFalse(encoder.matches(REG_PASSWORD,u.passwordHash));assertNotEquals(r.registrationPasswordHash,u.passwordHash);
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_type='registration' AND entity_id=? AND action='REGISTRATION_APPROVED'",Long.class,id));
  checkStatus(personal,REG_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.loginEmail").doesNotExist()).andExpect(jsonPath("$.loginPassword").doesNotExist());
  checkStatus(personal,"Wrong#Pass2026").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.loginPassword").doesNotExist());
  RegistrationRequest sealed=requests.findById(id).orElseThrow();assertNull(sealed.sealedLoginPassword);
  for(String col:List.of("credential_public_key","credential_private_key_enc","sealed_login_password","registration_password_hash"))assertFalse(String.valueOf(jdbc.queryForObject("SELECT "+col+" FROM registration_requests WHERE registration_id=?",String.class,id)).contains(LOGIN_PASSWORD));
  assertNull(sealed.credentialPrivateKeyEnc);
  Cookie token=login(login,LOGIN_PASSWORD);
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PERMIT_REVIEWER")).andExpect(jsonPath("$.email").value(login));
  mvc.perform(get("/api/permits/dashboard").cookie(token)).andExpect(status().isOk());
  mvc.perform(get("/api/admin/registrations").cookie(token)).andExpect(status().isForbidden());
  int regLogin=mvc.perform(postJson("/api/auth/login",Map.of("email",personal,"password",REG_PASSWORD),ip())).andReturn().getResponse().getStatus();assertTrue(regLogin==400||regLogin==401);
  mvc.perform(postJson("/api/auth/login",Map.of("email",login,"password",REG_PASSWORD),ip())).andExpect(status().isUnauthorized());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",loginEmail(),LOGIN_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isConflict());
 }
 @Test void T14_T16_duplicateLoginEmailRejectedAndRequestStaysPending()throws Exception{
  Long id=submit(email(),"APPLICANT");long before=users.count();Cookie adminCookie=adminToken();
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",member.email.toUpperCase(Locale.ROOT),LOGIN_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isConflict());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",admin.email,LOGIN_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isConflict());
  RegistrationRequest r=requests.findById(id).orElseThrow();assertEquals(RegistrationStatus.PENDING,r.status);assertNull(r.userId);assertEquals(before,users.count());
 }
 @Test void approvalRejectsReusedRegistrationPasswordAdminRoleAndBadLoginInput()throws Exception{
  Long id=submit(email(),"APPLICANT");Cookie adminCookie=adminToken();long before=users.count();
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",loginEmail(),REG_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isBadRequest());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("ADMIN",loginEmail(),LOGIN_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isBadRequest());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT","someone@gmail.com",LOGIN_PASSWORD),ip()).cookie(adminCookie)).andExpect(status().isBadRequest());
  mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",approval("APPLICANT",loginEmail(),"weakpassword"),ip()).cookie(adminCookie)).andExpect(status().isBadRequest());
  var mismatch=approval("APPLICANT",loginEmail(),LOGIN_PASSWORD);mismatch.put("confirmLoginPassword","Other#Pass2026");mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",mismatch,ip()).cookie(adminCookie)).andExpect(status().isBadRequest());
  var unconfirmed=approval("APPLICANT",loginEmail(),LOGIN_PASSWORD);unconfirmed.put("confirm",false);mvc.perform(postJson("/api/admin/registrations/"+id+"/approve",unconfirmed,ip()).cookie(adminCookie)).andExpect(status().isBadRequest());
  assertEquals(RegistrationStatus.PENDING,requests.findById(id).orElseThrow().status);assertEquals(before,users.count());
 }
 @Test void T21_rejectedRegistrationShowsReasonAndCreatesNoLogin()throws Exception{
  String personal=email();Long id=submit(personal,"APPLICANT");long before=users.count();
  mvc.perform(postJson("/api/admin/registrations/"+id+"/reject",Map.of("reason","Identity could not be confirmed","confirm",true),ip()).cookie(adminToken())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
  assertEquals(before,users.count());RegistrationRequest r=requests.findById(id).orElseThrow();assertNull(r.userId);assertEquals("Identity could not be confirmed",r.rejectionReason);
  checkStatus(personal,REG_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.message").value("Registration rejected. Contact Admin for assistance.")).andExpect(jsonPath("$.rejectionReason").value("Identity could not be confirmed"));
  int code=mvc.perform(postJson("/api/auth/login",Map.of("email",personal,"password",REG_PASSWORD),ip())).andReturn().getResponse().getStatus();assertTrue(code==400||code==401);
  assertThrows(ResponseStatusException.class,()->service.approve(id,new RegistrationDtos.Approve(Role.APPLICANT,loginEmail(),LOGIN_PASSWORD,LOGIN_PASSWORD,true),admin));
 }
 @Test void T22_T23_existingAdminUsersPermitsAndSupportUnchanged()throws Exception{
  String hash=users.findById(member.id).orElseThrow().passwordHash,adminHash=users.findById(admin.id).orElseThrow().passwordHash;
  long permits=jdbc.queryForObject("SELECT count(*) FROM applications",Long.class),history=jdbc.queryForObject("SELECT count(*) FROM workflow_history",Long.class),support=jdbc.queryForObject("SELECT count(*) FROM support_requests",Long.class);
  Long id=submit(email(),"APPLICANT");service.approve(id,new RegistrationDtos.Approve(Role.APPLICANT,loginEmail(),LOGIN_PASSWORD,LOGIN_PASSWORD,true),admin);
  Long rejected=submit(email(),"APPLICANT");service.reject(rejected,new RegistrationDtos.Reject("Isolated rejected fixture",true),admin);
  assertEquals(hash,users.findById(member.id).orElseThrow().passwordHash);assertEquals(adminHash,users.findById(admin.id).orElseThrow().passwordHash);
  assertEquals(permits,jdbc.queryForObject("SELECT count(*) FROM applications",Long.class));assertEquals(history,jdbc.queryForObject("SELECT count(*) FROM workflow_history",Long.class));assertEquals(support,jdbc.queryForObject("SELECT count(*) FROM support_requests",Long.class));
  mvc.perform(get("/api/auth/me").cookie(adminToken())).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
  mvc.perform(get("/api/auth/me").cookie(login(member.email,MEMBER_PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("APPLICANT"));
 }
 @Test void concurrentApprovalCreatesOneAccountAndOneAudit()throws Exception{
  Long id=submit(email(),"APPLICANT");long before=users.count();
  var outcomes=parallel(()->tryApprove(id,loginEmail()),()->tryApprove(id,loginEmail()));
  assertEquals(1,outcomes.stream().filter(Boolean::booleanValue).count());assertEquals(before+1,users.count());
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action='REGISTRATION_APPROVED' AND entity_type='registration' AND entity_id=?",Long.class,id));
 }
 boolean tryApprove(Long id,String login){try{service.approve(id,new RegistrationDtos.Approve(Role.APPLICANT,login,LOGIN_PASSWORD,LOGIN_PASSWORD,true),admin);return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}}
 @Test void concurrentApprovalsWithSameLoginEmailCreateOneUser()throws Exception{
  Long a=submit(email(),"APPLICANT"),b=submit(email(),"APPLICANT");String login=loginEmail();long before=users.count();
  var outcomes=parallel(()->tryApprove(a,login),()->tryApprove(b,login));
  assertEquals(1,outcomes.stream().filter(Boolean::booleanValue).count());assertEquals(before+1,users.count());
 }
 @Test void adminListingNeverReturnsPasswordHashesAndRequiresAdmin()throws Exception{
  submit(email(),"APPLICANT");mvc.perform(get("/api/admin/registrations")).andExpect(status().isUnauthorized());mvc.perform(get("/api/admin/registrations").cookie(login(member.email,MEMBER_PASSWORD))).andExpect(status().isForbidden());
  String body=mvc.perform(get("/api/admin/registrations").cookie(adminToken())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(body.contains("registrationPasswordHash"));assertFalse(body.contains("$2a$"));assertFalse(body.contains("$2b$"));assertFalse(body.contains(REG_PASSWORD));
  String roles=mvc.perform(get("/api/admin/registrations/roles").cookie(adminToken())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(roles.contains("\"ADMIN\""));
 }
 @Test void statusChecksAreRateLimitedPerSourceAndPerEmail()throws Exception{
  String source=ip();for(int i=0;i<30;i++)mvc.perform(postJson("/api/registrations/status",Map.of("personalEmail",email(),"registrationPassword","WrongPass#1"),source)).andExpect(status().isUnauthorized());
  mvc.perform(postJson("/api/registrations/status",Map.of("personalEmail",email(),"registrationPassword","WrongPass#1"),source)).andExpect(status().isTooManyRequests());
  String target=email();for(int i=0;i<30;i++)mvc.perform(postJson("/api/registrations/status",Map.of("personalEmail",target,"registrationPassword","WrongPass#1"),ip())).andExpect(status().isUnauthorized());
  mvc.perform(postJson("/api/registrations/status",Map.of("personalEmail",target,"registrationPassword","WrongPass#1"),ip())).andExpect(status().isTooManyRequests());
 }
 @Test void duplicatePendingRegistrationRejectedAndPublicMutationsNeedCsrf()throws Exception{
  String e=email();submit(e,"APPLICANT");mvc.perform(postJson("/api/registrations",input(e.toUpperCase(Locale.ROOT),"APPLICANT"),ip())).andExpect(status().isConflict());
  mvc.perform(post("/api/registrations").contentType("application/json").content(json.writeValueAsString(input(email(),"APPLICANT")))).andExpect(status().isForbidden());
  mvc.perform(postJson("/api/registrations/activate",Map.of("personalEmail",e),ip())).andExpect(status().is4xxClientError());
 }
 private <T>List<T> parallel(Callable<T>a,Callable<T>b)throws Exception{ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);try{var first=pool.submit(()->{start.await();return a.call();});var second=pool.submit(()->{start.await();return b.call();});start.countDown();return List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
}
