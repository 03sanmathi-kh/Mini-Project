package edu.kle.permits;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:support;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","app.jwt-secret=test-secret-with-at-least-thirty-two-bytes-long","app.admin-password=InitialAdminPassword!","app.admin-local-part=admin"})
@AutoConfigureMockMvc
class SupportIntegrationTest {
 @Autowired MockMvc mvc;@Autowired Users users;@Autowired SupportRequests requests;@Autowired PasswordEncoder encoder;@Autowired Events events;
 @MockitoBean Clock clock;final ObjectMapper json=new ObjectMapper();final Instant now=Instant.parse("2026-10-26T10:00:00Z");AppUser admin,applicant;
 @BeforeEach void setup(){when(clock.instant()).thenReturn(now);requests.deleteAll();admin=users.findByEmail("admin@permitworks.com").orElseThrow();admin.enabled=true;admin.locked=false;users.save(admin);applicant=users.findByEmail("support-user@permitworks.com").orElseGet(()->users.save(new AppUser("Support User","support-user@permitworks.com","contact@example.com",encoder.encode("SupportPass!"),Role.APPLICANT)));applicant.enabled=true;applicant.locked=true;users.save(applicant);}
 String request(String permitEmail,String personal){return "{\"fullName\":\"Support User\",\"personalContactEmail\":\""+personal+"\",\"permitworksEmail\":"+(permitEmail==null?"null":"\""+permitEmail+"\"")+",\"contactPhone\":\"+91 98765 43210\",\"category\":\"ACCOUNT_LOCKED\",\"subject\":\"Cannot sign in\",\"description\":\"Please help me regain access.\"}";}
 String loginBody(String email,String password){return "{\"email\":\""+email+"\",\"password\":\""+password+"\"}";}
 Cookie login(String email,String password)throws Exception{MvcResult r=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(loginBody(email,password))).andExpect(status().isOk()).andReturn();return r.getResponse().getCookie("PERMIT_TOKEN");}
 MvcResult submit(String body,String ip)throws Exception{return mvc.perform(post("/api/support").with(csrf()).with(r->{r.setRemoteAddr(ip);return r;}).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();}
 @Test void publicSubmissionWorksWithOrWithoutPermitworksEmailAndIsNeutral()throws Exception{
  MvcResult matched=submit(request("SUPPORT-USER@PERMITWORKS.COM","Contact@Example.COM"),"10.0.0.1");assertEquals(200,matched.getResponse().getStatus());JsonNode a=json.readTree(matched.getResponse().getContentAsString());assertEquals("Your support request has been submitted.",a.get("message").asText());assertTrue(a.get("ticketNumber").asText().startsWith("PW-HELP-2026-"));
  MvcResult missing=submit(request(null,"second@example.com"),"10.0.0.2");assertEquals(200,missing.getResponse().getStatus());JsonNode b=json.readTree(missing.getResponse().getContentAsString());assertEquals(a.get("message").asText(),b.get("message").asText());assertNull(requests.findAll().stream().filter(x->x.personalContactEmail.equals("second@example.com")).findFirst().orElseThrow().permitworksEmail);
 }
 @Test void validatesEmailsContentAndRateLimits()throws Exception{
  submit(request(null,"invalid"),"10.0.1.1").getResponse();mvc.perform(post("/api/support").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request(null,"invalid"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/support").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request("someone@gmail.com","valid@example.com"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/support").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request(null,"valid@example.com").replace("Please help me regain access.","<script>alert(1)</script>"))).andExpect(status().isBadRequest());
  submit(request(null,"first@example.com"),"10.0.2.1");MvcResult rapid=submit(request(null,"second@example.com"),"10.0.2.1");assertEquals(429,rapid.getResponse().getStatus());
 }
 @Test void publicStatusReturnsOnlySafeFieldsAndRequiresMatchingPersonalEmail()throws Exception{
  JsonNode created=json.readTree(submit(request("support-user@permitworks.com","contact@example.com"),"10.0.3.1").getResponse().getContentAsString());String ticket=created.get("ticketNumber").asText();SupportRequest entity=requests.findAll().get(0);entity.adminNotes="Never public";entity.userVisibleResponse="Please contact the permit office.";requests.save(entity);
  String check="{\"ticketNumber\":\""+ticket+"\",\"personalContactEmail\":\"CONTACT@EXAMPLE.COM\"}";
  mvc.perform(post("/api/support/status").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(check)).andExpect(status().isOk()).andExpect(jsonPath("$.userVisibleResponse").value("Please contact the permit office.")).andExpect(jsonPath("$.adminNotes").doesNotExist()).andExpect(jsonPath("$.matchedAccount").doesNotExist()).andExpect(jsonPath("$.personalContactEmail").doesNotExist());
  mvc.perform(post("/api/support/status").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(check.replace("CONTACT@EXAMPLE.COM","other@example.com"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("The ticket details could not be found."));
 }
 @Test void adminCanManageAndApplicantReceivesForbidden()throws Exception{
  submit(request("support-user@permitworks.com","contact@example.com"),"10.0.4.1");Long id=requests.findAll().get(0).id;Cookie adminToken=login("admin@permitworks.com","InitialAdminPassword!");applicant.locked=false;users.save(applicant);Cookie applicantToken=login("support-user@permitworks.com","SupportPass!");
  mvc.perform(get("/api/admin/support").cookie(applicantToken)).andExpect(status().isForbidden());mvc.perform(get("/api/admin/support").cookie(adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].ticketNumber").exists());mvc.perform(get("/api/admin/support/"+id).cookie(adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.adminNotes").hasJsonPath()).andExpect(jsonPath("$.matchedAccount.permitworksEmail").value("support-user@permitworks.com"));
  mvc.perform(patch("/api/admin/support/"+id).cookie(adminToken).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"adminNotes\":\"Identity checked\",\"userVisibleResponse\":\"Your account is ready.\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));assertTrue(events.countByAction("SUPPORT_REQUEST_RESOLVED")>0);
  applicant=users.findById(applicant.id).orElseThrow();applicant.locked=true;users.save(applicant);
  mvc.perform(post("/api/admin/support/"+id+"/unlock").cookie(adminToken).with(csrf())).andExpect(status().isOk());assertFalse(users.findById(applicant.id).orElseThrow().locked);assertTrue(events.countByAction("SUPPORT_ACCOUNT_UNLOCKED")>0);
 }
}
