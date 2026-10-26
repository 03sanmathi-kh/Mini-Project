package edu.kle.permits;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "app.jwt-secret=test-secret-with-at-least-thirty-two-bytes-long",
 "app.admin-password=InitialAdminPassword!","app.admin-local-part=admin"})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
 @Autowired MockMvc mvc; @Autowired Users users; @Autowired PasswordEncoder encoder;
 @Autowired Sessions sessions; @Autowired JwtDecoder decoder; @Autowired AuthService service;
 @MockitoBean Clock clock;
 final Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
 AppUser admin, applicant;
 @BeforeEach void setup(){
  when(clock.instant()).thenReturn(now);
  admin=users.findByEmail("admin@permitworks.com").orElseThrow();admin.locked=false;admin.failedAttempts=0;admin.enabled=true;users.save(admin);
  applicant=users.findByEmail("applicant@permitworks.com").orElseGet(()->users.save(new AppUser("Applicant Test","applicant@permitworks.com",encoder.encode("ApplicantPassword!"),Role.APPLICANT)));
  applicant.locked=false;applicant.failedAttempts=0;applicant.enabled=true;applicant.role=Role.APPLICANT;users.save(applicant);
 }
 org.springframework.test.web.servlet.request.RequestPostProcessor browserCsrf()throws Exception{
  var response=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
  Cookie cookie=response.getCookie("XSRF-TOKEN");assertNotNull(cookie);
  String token=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString()).get("token").asText();
  return request->{
   var cookies=new ArrayList<Cookie>();if(request.getCookies()!=null)cookies.addAll(Arrays.asList(request.getCookies()));cookies.removeIf(c->c.getName().equals("XSRF-TOKEN"));cookies.add(cookie);
   request.setCookies(cookies.toArray(Cookie[]::new));request.addHeader("X-XSRF-TOKEN",token);return request;
  };
 }
 String json(String email,String password){return "{\"email\":\""+email+"\",\"password\":\""+password+"\"}";}
 Cookie login(String email,String password)throws Exception{
  MvcResult r=mvc.perform(post("/api/auth/login").with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(json(email,password))).andExpect(status().isOk()).andReturn();
  Cookie c=r.getResponse().getCookie("PERMIT_TOKEN");assertNotNull(c);assertTrue(c.isHttpOnly());return c;
 }
 @Test void protectedEndpointsRequireAuthenticationAndCsrf()throws Exception{
  mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json("admin@permitworks.com","InitialAdminPassword!"))).andExpect(status().isForbidden());
 }
 @Test void locksOnFifthFailureAndOnlyAdminCanUnlock()throws Exception{
  for(int i=0;i<5;i++)mvc.perform(post("/api/auth/login").with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(json("applicant@permitworks.com","wrongpass"))).andExpect(status().isUnauthorized());
  assertTrue(users.findById(applicant.id).orElseThrow().locked);
  mvc.perform(post("/api/auth/login").with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(json("applicant@permitworks.com","ApplicantPassword!"))).andExpect(status().isUnauthorized());
  Cookie token=login("admin@permitworks.com","InitialAdminPassword!");
  mvc.perform(post("/api/admin/users/"+applicant.id+"/unlock").cookie(token).with(browserCsrf())).andExpect(status().isOk());
  assertEquals(0,users.findById(applicant.id).orElseThrow().failedAttempts);
  login("applicant@permitworks.com","ApplicantPassword!");
 }
 @Test void rolesAndTokensCannotBeBypassed()throws Exception{
  Cookie token=login("applicant@permitworks.com","ApplicantPassword!");
  mvc.perform(get("/api/admin/users").cookie(token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("APPLICANT")).andExpect(jsonPath("$.passwordHash").doesNotExist());
  mvc.perform(get("/api/auth/me").cookie(new Cookie("PERMIT_TOKEN",token.getValue()+"tampered"))).andExpect(status().isUnauthorized());
 }
 @Test void logoutRevokesToken()throws Exception{
  Cookie token=login("applicant@permitworks.com","ApplicantPassword!");
  mvc.perform(post("/api/auth/logout").cookie(token).with(browserCsrf())).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isUnauthorized());
 }
 @Test void idleTimeoutUsesServerActivityAndPollingDoesNotExtendIt()throws Exception{
  Cookie token=login("applicant@permitworks.com","ApplicantPassword!");
  when(clock.instant()).thenReturn(now.plusSeconds(1799));
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk());
  when(clock.instant()).thenReturn(now.plusSeconds(1800));
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isUnauthorized());
 }
 @Test void explicitActivityExtendsIdleWindowButNotAbsoluteLifetime()throws Exception{
  Cookie token=login("applicant@permitworks.com","ApplicantPassword!");
  when(clock.instant()).thenReturn(now.plusSeconds(1200));
  mvc.perform(post("/api/auth/activity").cookie(token).with(browserCsrf())).andExpect(status().isOk());
  when(clock.instant()).thenReturn(now.plusSeconds(2400));
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk());
  when(clock.instant()).thenReturn(now.plusSeconds(28800));
  mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isUnauthorized());
 }
 @Test void adminCreationValidationAndRoleChanges()throws Exception{
  Cookie adminToken=login("admin@permitworks.com","InitialAdminPassword!");
  String body="{\"fullName\":\"New User\",\"localPart\":\"New-User\",\"personalContactEmail\":\"New.User@Example.COM\",\"password\":\"LongEnoughPassword!\",\"role\":\"APPLICANT\"}";
  // Unique per test database; this test runs once.
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  AppUser created=users.findByEmail("new-user@permitworks.com").orElseThrow();
  assertNotEquals("LongEnoughPassword!",created.passwordHash);assertTrue(encoder.matches("LongEnoughPassword!",created.passwordHash));assertEquals("new.user@example.com",created.personalContactEmail);
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("APPLICANT","UNKNOWN"))).andExpect(status().isBadRequest());
  Cookie applicantToken=login("applicant@permitworks.com","ApplicantPassword!");
  mvc.perform(patch("/api/admin/users/"+applicant.id).cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Changed\",\"role\":\"FINANCE_OFFICER\",\"enabled\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").cookie(applicantToken)).andExpect(status().isUnauthorized());
 }

 @Test void passwordByteLimitsAndExistingHashesRemainStable()throws Exception{
  Cookie adminToken=login("admin@permitworks.com","InitialAdminPassword!");String originalHash=admin.passwordHash;
  String base="{\"fullName\":\"Boundary User\",\"localPart\":\"%s\",\"personalContactEmail\":\"%s@example.com\",\"password\":\"%s\",\"role\":\"APPLICANT\"}";
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(String.format(base,"short","short","1234567"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(String.format(base,"eight","eight","12345678"))).andExpect(status().isOk());
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(String.format(base,"seventy-two","seventytwo","a".repeat(72)))).andExpect(status().isOk());
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(String.format(base,"too-long","toolong","a".repeat(73)))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/admin/users").cookie(adminToken).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(String.format(base,"unicode-long","unicodelong","é".repeat(37)))).andExpect(status().isBadRequest());
  assertEquals(originalHash,users.findById(admin.id).orElseThrow().passwordHash);
 }

 @Test void personalEmailNeverAuthenticatesAndLegacyNullIsAllowed()throws Exception{
  applicant.personalContactEmail="person@example.com";users.save(applicant);
  mvc.perform(post("/api/auth/login").with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content(json("person@example.com","ApplicantPassword!"))).andExpect(status().isBadRequest());
  AppUser legacy=users.save(new AppUser("Legacy User","legacy@permitworks.com",null,encoder.encode("LegacyPass!"),Role.APPLICANT));assertNull(legacy.personalContactEmail);
  login("legacy@permitworks.com","LegacyPass!");
 }
 @Test void allEightRolesEnforceAdminBoundary()throws Exception{
  for(Role role:Role.values()){
   String email="role-"+role.name().toLowerCase()+"@permitworks.com";
   if(!users.existsByEmailIgnoreCase(email))users.save(new AppUser("Role Test",email,encoder.encode("RoleTestPassword!"),role));
   Cookie token=login(email,"RoleTestPassword!");
   mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()));
   mvc.perform(get("/api/admin/users").cookie(token)).andExpect(status().is(role==Role.ADMIN?200:403));
  }
 }

 @Test void browserCsrfCookieAndHeaderAllowLogin()throws Exception{
  var r=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
  Cookie cookie=r.getResponse().getCookie("XSRF-TOKEN");assertNotNull(cookie);
  String value=new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.getResponse().getContentAsString()).get("token").asText();
  mvc.perform(post("/api/auth/login").cookie(cookie).header("X-XSRF-TOKEN",value).contentType(MediaType.APPLICATION_JSON).content(json("admin@permitworks.com","InitialAdminPassword!"))).andExpect(status().isOk());
 }
 @Test void disablingRevokesSessionsAndSelfDisableIsRejected()throws Exception{
  Cookie a=login("admin@permitworks.com","InitialAdminPassword!"),u=login("applicant@permitworks.com","ApplicantPassword!");
  mvc.perform(patch("/api/admin/users/"+applicant.id).cookie(a).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Applicant Test\",\"role\":\"APPLICANT\",\"enabled\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").cookie(u)).andExpect(status().isUnauthorized());
  mvc.perform(patch("/api/admin/users/"+admin.id).cookie(a).with(browserCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Admin\",\"role\":\"ADMIN\",\"enabled\":false}")).andExpect(status().isBadRequest());
 }
}
