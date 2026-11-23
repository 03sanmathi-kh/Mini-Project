package edu.kle.permits;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={
 "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:engines;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000}",
 "spring.datasource.driver-class-name=${TEST_DB_DRIVER:org.h2.Driver}","spring.datasource.username=${TEST_DB_USER:sa}","spring.datasource.password=${TEST_DB_PASSWORD:}",
 "app.jwt-secret=test-secret-with-at-least-thirty-two-bytes-long","app.admin-password=InitialAdminPassword!","app.admin-local-part=admin"})
@AutoConfigureMockMvc
class EngineIntegrationTest {
 @Autowired PermitService service;@Autowired AssignmentEngine routing;@Autowired Users users;@Autowired Assignments assignments;
 @Autowired PermitRecords permits;@Autowired Notifications notices;@Autowired WorkflowHistories history;@Autowired RoutingProfiles profiles;
 @Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager transactions;@Autowired MockMvc mvc;@Autowired DocumentVersions documents;
 @MockitoSpyBean NotificationService notifications;
 @MockitoSpyBean SmsProvider sms;
 AppUser applicant,department,reviewer,zoning,finance,inspector,senior;String type;
 @BeforeEach void setup(){type="T"+UUID.randomUUID().toString().substring(0,8);applicant=user(Role.APPLICANT);department=user(Role.DEPARTMENT_OFFICER);reviewer=user(Role.PERMIT_REVIEWER);zoning=user(Role.ZONING_OFFICER);finance=user(Role.FINANCE_OFFICER);inspector=user(Role.BUILDING_INSPECTOR);senior=user(Role.SENIOR_DEPARTMENT_OFFICER);}
 AppUser user(Role role){return users.save(new AppUser("Engine test "+role,UUID.randomUUID()+"@permitworks.com","not-an-authentication-fixture",role));}
 PermitDtos.ApplicationInput input(String title){return new PermitDtos.ApplicationInput(title,type,"Review test address","Construction","Owner","Architect",BigDecimal.TEN,LocalDate.of(2027,1,1),"reference.pdf","NORTH");}
 Long ready(){var p=service.create(input("Engine test"),applicant);service.submit(p.id(),applicant);service.verifyDocuments(p.id(),new PermitDtos.DocumentDecision(true,"Verified"),department);return p.id();}
 void profile(AppUser officer,AssignmentType stage,Integer hours){routing.profile(new AssignmentEngine.ProfileInput(officer.id,type,"NORTH",stage,hours),department);}
 void reviewed(Long id){service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,reviewer.id),department);service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.ZONING_OFFICER,zoning.id),department);service.review(id,ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"OK"),reviewer);service.review(id,ReviewType.ZONING,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"OK"),zoning);}
 @Test void TC_E4_01_leastLoadedEligibleOfficerAndLowestIdTie(){
  AppUser second=user(Role.PERMIT_REVIEWER);profile(reviewer,AssignmentType.PERMIT_REVIEWER,24);profile(second,AssignmentType.PERMIT_REVIEWER,24);
  Long a=ready(),b=ready();assertEquals(reviewer.id,service.automatic(a,AssignmentType.PERMIT_REVIEWER,department).officerId());assertEquals(second.id,service.automatic(b,AssignmentType.PERMIT_REVIEWER,department).officerId());
  assertEquals(reviewer.id,service.automatic(a,AssignmentType.PERMIT_REVIEWER,department).officerId());
 }
 @Test void TC_E4_02_noEligibleOfficerQueuesAndAlertsWithoutStateCorruption(){
  Long id=ready();var result=service.automatic(id,AssignmentType.PERMIT_REVIEWER,department);assertFalse(result.assigned());assertEquals(PermitStatus.DOCUMENT_VERIFICATION,permits.findById(id).orElseThrow().status);
  assertTrue(routing.report(department).unassigned().stream().anyMatch(q->q.applicationId.equals(id)));
  assertTrue(notices.findTop100ByRecipientIdOrderByCreatedAtDesc(department.id).stream().anyMatch(n->n.event.equals("UNASSIGNED")));
 }
 @Test void TC_E4_03_overrideNeedsRoleConfirmationReasonAndPreservesPrevious(){
  Long id=ready();service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,reviewer.id),department);AppUser second=user(Role.PERMIT_REVIEWER);
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,second.id),department)).getStatusCode().value());
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,second.id,true,"Redistribute"),applicant)).getStatusCode().value());
  service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,second.id,true,"Redistribute"),department);
  var a=assignments.findFirstByApplicationIdAndAssignmentTypeAndActiveTrue(id,AssignmentType.PERMIT_REVIEWER).orElseThrow();assertEquals(reviewer.id,a.previousAssigneeId);assertEquals("Redistribute",a.overrideReason);
 }
 @Test void TC_E4_04_overdueUsesConfiguredDeadlineAndReportsUnconfigured(){
  profile(reviewer,AssignmentType.PERMIT_REVIEWER,1);Long id=ready();service.automatic(id,AssignmentType.PERMIT_REVIEWER,department);
  jdbc.update("UPDATE application_assignments SET due_at=? WHERE application_id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),id);
  assertTrue(routing.report(department).assignments().stream().anyMatch(a->a.applicationId().equals(id)&&a.overdue()));
  Long other=ready();service.assign(other,new PermitDtos.AssignmentInput(AssignmentType.ZONING_OFFICER,zoning.id),department);assertTrue(routing.report(department).deadlinesUnconfigured()>0);
 }
 @Test void TC_E4_05_workloadApiIsRoleRestricted()throws Exception{
  mvc.perform(get("/api/reports/workload").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("applicant").roles("APPLICANT")).requestAttr("currentUser",applicant)).andExpect(status().isForbidden());
  mvc.perform(get("/api/reports/workload").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("department").roles("DEPARTMENT_OFFICER")).requestAttr("currentUser",department)).andExpect(status().isOk()).andExpect(jsonPath("$.officers").isArray());
 }
 @Test void TC_E4_06_concurrentLeastLoadDecisionsAreSerialized()throws Exception{
  AppUser second=user(Role.PERMIT_REVIEWER);profile(reviewer,AssignmentType.PERMIT_REVIEWER,1);profile(second,AssignmentType.PERMIT_REVIEWER,1);Long a=ready(),b=ready();
  var results=parallel(()->service.automatic(a,AssignmentType.PERMIT_REVIEWER,department).officerId(),()->service.automatic(b,AssignmentType.PERMIT_REVIEWER,department).officerId());assertEquals(2,new HashSet<>(results).size());
  var same=parallel(()->service.automatic(a,AssignmentType.PERMIT_REVIEWER,department).officerId(),()->service.automatic(a,AssignmentType.PERMIT_REVIEWER,department).officerId());assertEquals(same.get(0),same.get(1));assertEquals(1,assignments.findByApplicationIdOrderByAssignedAt(a).stream().filter(x->x.active&&x.assignmentType==AssignmentType.PERMIT_REVIEWER).count());
 }
 @Test void TC_E4_07_invalidAndMismatchedRoutingIsRejected(){
  assertThrows(ResponseStatusException.class,()->routing.profile(new AssignmentEngine.ProfileInput(applicant.id,type,"NORTH",AssignmentType.PERMIT_REVIEWER,1),department));
  routing.profile(new AssignmentEngine.ProfileInput(reviewer.id,type,"SOUTH",AssignmentType.PERMIT_REVIEWER,1),department);assertFalse(service.automatic(ready(),AssignmentType.PERMIT_REVIEWER,department).assigned());
  profile(reviewer,AssignmentType.PERMIT_REVIEWER,1);reviewer.locked=true;users.save(reviewer);assertFalse(service.automatic(ready(),AssignmentType.PERMIT_REVIEWER,department).assigned());
 }
 @Test void TC_E4_08_reportFiltersMatchActualCounts(){
  Long id=ready();service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,reviewer.id),department);
  var report=routing.report(department,LocalDate.now(ZoneOffset.UTC),LocalDate.now(ZoneOffset.UTC),type);assertEquals(1,report.pending());assertEquals(1,report.assignments().size());
  assertEquals(0,routing.report(department,null,null,"ABSENT-TYPE").pending());assertThrows(ResponseStatusException.class,()->routing.report(department,LocalDate.of(2027,1,2),LocalDate.of(2027,1,1),null));
 }
 @Test void TC_E3_01_failureAfterMultipleWritesRollsBackEverything(){
  long before=permits.count(),hist=history.count(),count=jdbc.queryForObject("SELECT count(*) FROM audit_log",Long.class);
  doThrow(new IllegalStateException("deliberate isolated failure")).when(notifications).workflow(any(),eq("DRAFT_CREATED"));
  assertThrows(IllegalStateException.class,()->service.create(input("Must roll back"),applicant));
  assertEquals(before,permits.count());assertEquals(hist,history.count());assertEquals(count,jdbc.queryForObject("SELECT count(*) FROM audit_log",Long.class));
 }
 @Test void TC_E3_02_foreignKeysRejectAllRelevantInvalidReferences(){
  String[] sql={
   "UPDATE permit_applications SET applicant_id=-999 WHERE id=?",
   "INSERT INTO application_assignments(application_id,assignment_type,assignee_id,assigned_by,assigned_at,active) VALUES (?,'PERMIT_REVIEWER',-999,-999,CURRENT_TIMESTAMP,TRUE)",
   "INSERT INTO payment_records(application_id,fee_amount,payment_status,recorded_by,recorded_at) VALUES (?,10,'PAID',-999,CURRENT_TIMESTAMP)",
   "INSERT INTO application_reviews(application_id,review_type,reviewer_id,recommendation,review_date,review_round) VALUES (?,'TECHNICAL',-999,'APPROVE',CURRENT_TIMESTAMP,1)",
   "INSERT INTO inspections(application_id,inspector_id,inspection_type,scheduled_date,result,final_inspection,created_at,updated_at) VALUES (?,-999,'Final',CURRENT_DATE,'SCHEDULED',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"};
  Long id=ready();for(String statement:sql)assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->new TransactionTemplate(transactions).execute(s->{jdbc.update(statement,id);return null;}));
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->new TransactionTemplate(transactions).execute(s->{jdbc.update("INSERT INTO application_assignments(application_id,assignment_type,assignee_id,assigned_by,assigned_at,active) VALUES (-999,'PERMIT_REVIEWER',?,?,CURRENT_TIMESTAMP,TRUE)",reviewer.id,department.id);return null;}));
 }
 @Test void TC_E3_03_simultaneousReviewProducesOneRecommendation()throws Exception{
  Long id=ready();service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,reviewer.id),department);service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.ZONING_OFFICER,zoning.id),department);
  var r=parallel(()->tryReview(id),()->tryReview(id));assertEquals(1,r.stream().filter(Boolean::booleanValue).count());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM application_reviews WHERE application_id=?",Long.class,id));
 }
 boolean tryReview(Long id){try{service.review(id,ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Concurrent"),reviewer);return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}}
 @Test void TC_E3_04_simultaneousPaymentsProduceOnePaymentAndIssueOnce()throws Exception{
  Long id=ready();reviewed(id);service.decide(id,new PermitDtos.DecisionInput("APPROVE","OK"),senior);
  var r=parallel(()->tryPayment(id),()->tryPayment(id));assertEquals(1,r.stream().filter(Boolean::booleanValue).count());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM payment_records WHERE application_id=?",Long.class,id));
 }
 boolean tryPayment(Long id){try{service.payment(id,new PermitDtos.PaymentInput(BigDecimal.TEN,PaymentStatus.PAID,"TEST-CONCURRENT"),finance);return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}}
 @Test void TC_E2_01_unchangedResubmissionRejectedButActualEditAccepted(){
  var p=service.create(input("Original"),applicant);service.submit(p.id(),applicant);service.verifyDocuments(p.id(),new PermitDtos.DocumentDecision(false,"Need correction"),department);
  assertEquals(409,assertThrows(ResponseStatusException.class,()->service.resubmit(p.id(),new PermitDtos.Resubmit("No real change"),applicant)).getStatusCode().value());
  service.edit(p.id(),input("Corrected"),applicant);assertEquals(2,service.resubmit(p.id(),new PermitDtos.Resubmit("Changed title"),applicant).reviewRound());
 }
 @Test void TC_E2_02_incompleteInspectionRequiresReasonAndCannotClose(){
  Long id=ready();reviewed(id);service.decide(id,new PermitDtos.DecisionInput("APPROVE","OK"),senior);service.payment(id,new PermitDtos.PaymentInput(BigDecimal.TEN,PaymentStatus.PAID,"INCOMPLETE-TEST"),finance);
  service.assign(id,new PermitDtos.AssignmentInput(AssignmentType.BUILDING_INSPECTOR,inspector.id),department);
  var p=service.schedule(id,new PermitDtos.InspectionInput("Final",LocalDate.now(),true),inspector);Long inspection=p.inspections().get(0).id();
  var result=service.recordInspection(id,inspection,new PermitDtos.InspectionResultInput(LocalDate.now(),"Site inaccessible",InspectionResult.COULD_NOT_COMPLETE),inspector);
  assertEquals(PermitStatus.PERMIT_ISSUED,result.status());assertThrows(ResponseStatusException.class,()->service.close(id,inspector));
  assertEquals(PermitStatus.INSPECTION_SCHEDULED,service.schedule(id,new PermitDtos.InspectionInput("Reinspection",LocalDate.now().plusDays(1),true),inspector).status());
 }
 @Test void TC_E2_03_smsFailureStillPersistsInAppNotificationAndRecipientIsolation()throws Exception{
  doThrow(new IllegalStateException("isolated mock failure")).when(sms).send(any(),any());
  var p=service.create(input("Notification fallback"),applicant);var n=notices.findTop100ByRecipientIdOrderByCreatedAtDesc(applicant.id).get(0);assertEquals("FAILED",n.smsStatus);
  mvc.perform(post("/api/notifications/"+n.id+"/read").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("other").roles("DEPARTMENT_OFFICER")).requestAttr("currentUser",department).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(post("/api/notifications/"+n.id+"/read").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner").roles("APPLICANT")).requestAttr("currentUser",applicant).with(csrf())).andExpect(status().isOk());assertNotNull(notices.findById(n.id).orElseThrow().readAt);
 }
 @Test void TC_E3_05_secureDocumentVersionsKeepPriorBytesAndEnforceOwnership()throws Exception{
  var p=service.create(input("Document versions"),applicant);
  for(int v=1;v<=2;v++)mvc.perform(multipart("/api/permits/"+p.id()+"/documents/files").file(new MockMultipartFile("file","plan.pdf","application/pdf",("%PDF-1.4 fixture version "+v).getBytes())).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner").roles("APPLICANT")).requestAttr("currentUser",applicant).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(v));
  var records=documents.findByApplicationIdOrderByUploadedAt(p.id());assertEquals(2,records.size());assertNotEquals(records.get(0).contentHash,records.get(1).contentHash);
  mvc.perform(get("/api/permits/"+p.id()+"/documents/files/"+records.get(0).id).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("other").roles("APPLICANT")).requestAttr("currentUser",user(Role.APPLICANT))).andExpect(status().isNotFound());
  mvc.perform(multipart("/api/permits/"+p.id()+"/documents/files").file(new MockMultipartFile("file","../unsafe.html","text/html","<script>bad</script>".getBytes())).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("owner").roles("APPLICANT")).requestAttr("currentUser",applicant).with(csrf())).andExpect(status().isBadRequest());
 }
 @Test void TC_E3_06_concurrentSubmissionCreatesExactlyOneVersion()throws Exception{
  Long id=service.create(input("Concurrent submission"),applicant).id();var results=parallel(()->trySubmit(id),()->trySubmit(id));assertEquals(1,results.stream().filter(Boolean::booleanValue).count());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM application_versions WHERE application_id=?",Long.class,id));
 }
 boolean trySubmit(Long id){try{service.submit(id,applicant);return true;}catch(ResponseStatusException e){assertEquals(409,e.getStatusCode().value());return false;}}
 @Test void TC_E3_07_rollbackRecoveryTimeIsMeasuredWithoutClaimingCrashRecovery(){
  Long id=ready();String original=permits.findById(id).orElseThrow().projectTitle;long start=System.nanoTime();
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(s->{jdbc.update("UPDATE permit_applications SET project_title='must rollback' WHERE id=?",id);throw new IllegalStateException("isolated rollback");}));
  assertEquals(original,permits.findById(id).orElseThrow().projectTitle);System.out.println("ROLLBACK_RECOVERY_MS="+(System.nanoTime()-start)/1_000_000.0+"; crash recovery NOT tested");
 }
 @Test void TC_E3_08_postgresqlRejectsAuditMutation(){
  Assumptions.assumeTrue(jdbc.execute((java.sql.Connection c)->c.getMetaData().getDatabaseProductName()).equals("PostgreSQL"),"PostgreSQL-specific trigger test");
  Long id=ready();assertThrows(org.springframework.dao.DataAccessException.class,()->new TransactionTemplate(transactions).execute(s->{jdbc.update("UPDATE audit_log SET details='tampered' WHERE entity_id=? AND entity_type='permit'",id);return null;}));
 }
 @Test void TC_E3_09_legacyCompletionBackfillUsesCurrentReviewRoundOnly(){
  Long id=ready();reviewed(id);jdbc.update("UPDATE application_assignments SET completed=FALSE WHERE application_id=?",id);
  assertEquals(2,routing.report(department,null,null,type).assignments().size());long historyCount=history.count();
  var migration=new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("db/migration/V9__backfill_finished_legacy_assignments.sql"));migration.execute(jdbc.getDataSource());
  assertEquals(0,routing.report(department,null,null,type).assignments().size());assertEquals(historyCount,history.count());
  service.decide(id,new PermitDtos.DecisionInput("CORRECTION","Update required"),senior);service.edit(id,input("Actual second round edit"),applicant);service.resubmit(id,new PermitDtos.Resubmit("Changed"),applicant);
  migration.execute(jdbc.getDataSource());assertEquals(2,routing.report(department,null,null,type).assignments().size());
 }
 private <T> List<T> parallel(Callable<T> a,Callable<T> b)throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
  try{Future<T> first=pool.submit(()->{start.await();return a.call();});Future<T> second=pool.submit(()->{start.await();return b.call();});start.countDown();return List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
 }
}
