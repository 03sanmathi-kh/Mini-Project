package edu.kle.permits;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "app.jwt-secret=test-secret-with-at-least-thirty-two-bytes-long",
 "app.admin-password=InitialAdminPassword!","app.admin-local-part=admin"})
class PermitWorkflowIntegrationTest {
 @Autowired PermitService service;@Autowired Users users;@Autowired PasswordEncoder encoder;@Autowired WorkflowHistories histories;
 AppUser applicant,other,department,reviewer,otherReviewer,zoning,senior,finance,inspector;
 @BeforeEach void setup(){applicant=user("flow-applicant",Role.APPLICANT);other=user("flow-other",Role.APPLICANT);department=user("flow-department",Role.DEPARTMENT_OFFICER);reviewer=user("flow-reviewer",Role.PERMIT_REVIEWER);otherReviewer=user("flow-reviewer-2",Role.PERMIT_REVIEWER);zoning=user("flow-zoning",Role.ZONING_OFFICER);senior=user("flow-senior",Role.SENIOR_DEPARTMENT_OFFICER);finance=user("flow-finance",Role.FINANCE_OFFICER);inspector=user("flow-inspector",Role.BUILDING_INSPECTOR);}
 AppUser user(String local,Role role){String email=local+"@permitworks.com";AppUser u=users.findByEmail(email).orElseGet(()->users.save(new AppUser(local,email,encoder.encode("ValidPassword!"),role)));u.role=role;u.enabled=true;u.locked=false;return users.save(u);}
 PermitDtos.ApplicationInput input(String title){return new PermitDtos.ApplicationInput(title,"Residential", "1 Permit Lane","Two-storey construction","Property Owner","Architect Studio",new BigDecimal("250000.00"),LocalDate.now().plusMonths(1),"plan.pdf, ownership.pdf");}

 @Test void emailRulesNormalizeAndRejectExternalDomains(){assertEquals("amruta@permitworks.com",PermitEmails.fromLocalPart(" Amruta "));assertEquals("admin@permitworks.com",PermitEmails.normalizeFull(" ADMIN@PERMITWORKS.COM "));assertThrows(ResponseStatusException.class,()->PermitEmails.fromLocalPart("a@other.com"));assertThrows(ResponseStatusException.class,()->PermitEmails.normalizeFull("a@gmail.com"));}
 @Test void migratedBootstrapAdminUsesPermitworksEmail(){assertTrue(users.findByEmail("admin@permitworks.com").isPresent());}
 @Test void completeCorrectionPaymentInspectionFlowEnforcesOwnershipAndAssignment(){
  var p=service.create(input("Library renovation"),applicant);service.submit(p.id(),applicant);
  ResponseStatusException hidden=assertThrows(ResponseStatusException.class,()->service.get(p.id(),other));assertEquals(HttpStatus.NOT_FOUND,hidden.getStatusCode());
  service.verifyDocuments(p.id(),new PermitDtos.DocumentDecision(true,"Complete"),department);
  service.assign(p.id(),new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,reviewer.id),department);
  service.assign(p.id(),new PermitDtos.AssignmentInput(AssignmentType.ZONING_OFFICER,zoning.id),department);
  assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,()->service.review(p.id(),ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Looks good"),otherReviewer)).getStatusCode());
  service.review(p.id(),ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Structure compliant"),reviewer);
  service.review(p.id(),ReviewType.ZONING,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Zoning compliant"),zoning);
  service.decide(p.id(),new PermitDtos.DecisionInput("CORRECTION","Clarify emergency access"),senior);
  service.edit(p.id(),input("Library renovation with emergency access"),applicant);
  service.resubmit(p.id(),new PermitDtos.Resubmit("Emergency access added to plan"),applicant);
  service.review(p.id(),ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Revised plan compliant"),reviewer);
  service.review(p.id(),ReviewType.ZONING,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Still compliant"),zoning);
  service.decide(p.id(),new PermitDtos.DecisionInput("APPROVE","Approved"),senior);
  assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,()->service.payment(p.id(),new PermitDtos.PaymentInput(BigDecimal.TEN,PaymentStatus.PAID,"X"),senior)).getStatusCode());
  service.payment(p.id(),new PermitDtos.PaymentInput(new BigDecimal("1500"),PaymentStatus.PAID,"SIM-001"),finance);
  service.assign(p.id(),new PermitDtos.AssignmentInput(AssignmentType.BUILDING_INSPECTOR,inspector.id),department);
  var scheduled=service.schedule(p.id(),new PermitDtos.InspectionInput("Final inspection",LocalDate.now().plusDays(1),true),inspector);
  Long inspectionId=scheduled.inspections().get(0).id();service.recordInspection(p.id(),inspectionId,new PermitDtos.InspectionResultInput(LocalDate.now().plusDays(1),"All work conforms",InspectionResult.PASSED),inspector);
  var closed=service.close(p.id(),inspector);assertEquals(PermitStatus.PROJECT_CLOSED,closed.status());assertTrue(closed.history().size()>=14);assertEquals(2,closed.reviewRound());
 }
 @Test void invalidTransitionReturnsConflictAndCreatesNoFalseTransition(){var p=service.create(input("Invalid transition test"),applicant);long before=histories.count();ResponseStatusException ex=assertThrows(ResponseStatusException.class,()->service.decide(p.id(),new PermitDtos.DecisionInput("APPROVE",null),senior));assertEquals(HttpStatus.CONFLICT,ex.getStatusCode());assertEquals(before,histories.count());}
 @Test void seniorCanRejectOnlyAfterBothRecommendations(){var p=service.create(input("Rejection path"),applicant);service.submit(p.id(),applicant);service.verifyDocuments(p.id(),new PermitDtos.DocumentDecision(true,"Complete"),department);service.assign(p.id(),new PermitDtos.AssignmentInput(AssignmentType.PERMIT_REVIEWER,reviewer.id),department);service.assign(p.id(),new PermitDtos.AssignmentInput(AssignmentType.ZONING_OFFICER,zoning.id),department);service.review(p.id(),ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Reviewed"),reviewer);service.review(p.id(),ReviewType.ZONING,new PermitDtos.ReviewInput(ReviewRecommendation.APPROVE,"Reviewed"),zoning);var rejected=service.decide(p.id(),new PermitDtos.DecisionInput("REJECT","Unsafe design"),senior);assertEquals(PermitStatus.REJECTED,rejected.status());assertEquals("Unsafe design",rejected.history().get(rejected.history().size()-1).remarks());}
}
