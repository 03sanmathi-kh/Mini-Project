package edu.kle.permits;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

enum PermitStatus {DRAFT,SUBMITTED,DOCUMENT_VERIFICATION,ASSIGNED,TECHNICAL_REVIEW,ZONING_REVIEW,CORRECTION_REQUIRED,RESUBMITTED,PENDING_FINAL_DECISION,APPROVED,REJECTED,PAYMENT_PENDING,PERMIT_ISSUED,INSPECTION_SCHEDULED,INSPECTION_FAILED,INSPECTION_PASSED,PROJECT_CLOSED}
enum AssignmentType {PERMIT_REVIEWER,ZONING_OFFICER,BUILDING_INSPECTOR}
enum ReviewType {TECHNICAL,ZONING}
enum ReviewRecommendation {APPROVE,CORRECTION_REQUIRED}
enum PaymentStatus {PENDING,PAID}
enum InspectionResult {SCHEDULED,PASSED,FAILED}

@Entity @Table(name="permit_applications")
class PermitRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String permitNumber;
 @Column(nullable=false) Long applicantId;
 String projectTitle;String permitType;String propertyAddress;
 @Column(length=2000) String constructionDescription;
 String propertyOwnerName;String contractorArchitectName;
 BigDecimal estimatedProjectCost;LocalDate proposedStartDate;
 @Column(length=2000) String supportingDocuments;
 @Enumerated(EnumType.STRING) PermitStatus status=PermitStatus.DRAFT;
 int reviewRound=1;Instant createdAt;Instant updatedAt;
 @Version long version;
 protected PermitRecord(){}
 PermitRecord(Long owner,PermitDtos.ApplicationInput d,Instant now){applicantId=owner;createdAt=updatedAt=now;apply(d);}
 void apply(PermitDtos.ApplicationInput d){projectTitle=d.projectTitle().trim();permitType=d.permitType().trim();propertyAddress=d.propertyAddress().trim();constructionDescription=d.constructionDescription().trim();propertyOwnerName=d.propertyOwnerName().trim();contractorArchitectName=d.contractorArchitectName().trim();estimatedProjectCost=d.estimatedProjectCost();proposedStartDate=d.proposedStartDate();supportingDocuments=d.supportingDocuments().trim();}
}
@Entity @Table(name="application_versions") class ApplicationVersion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;int reviewRound;String projectTitle;String permitType;String propertyAddress;@Column(length=2000)String constructionDescription;String propertyOwnerName;String contractorArchitectName;BigDecimal estimatedProjectCost;LocalDate proposedStartDate;@Column(length=2000)String supportingDocuments;Instant submittedAt;
 protected ApplicationVersion(){} ApplicationVersion(PermitRecord p,Instant now){applicationId=p.id;reviewRound=p.reviewRound;projectTitle=p.projectTitle;permitType=p.permitType;propertyAddress=p.propertyAddress;constructionDescription=p.constructionDescription;propertyOwnerName=p.propertyOwnerName;contractorArchitectName=p.contractorArchitectName;estimatedProjectCost=p.estimatedProjectCost;proposedStartDate=p.proposedStartDate;supportingDocuments=p.supportingDocuments;submittedAt=now;}
}
@Entity @Table(name="application_assignments") class ApplicationAssignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;@Enumerated(EnumType.STRING)AssignmentType assignmentType;Long assigneeId;Long assignedBy;Instant assignedAt;boolean active=true;
 protected ApplicationAssignment(){} ApplicationAssignment(Long app,AssignmentType type,Long assignee,Long actor,Instant now){applicationId=app;assignmentType=type;assigneeId=assignee;assignedBy=actor;assignedAt=now;}
}
@Entity @Table(name="application_reviews") class ApplicationReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;@Enumerated(EnumType.STRING)ReviewType reviewType;Long reviewerId;@Enumerated(EnumType.STRING)ReviewRecommendation recommendation;@Column(length=2000)String remarks;Instant reviewDate;int reviewRound;
 protected ApplicationReview(){} ApplicationReview(PermitRecord p,ReviewType type,Long reviewer,ReviewRecommendation result,String note,Instant now){applicationId=p.id;reviewType=type;reviewerId=reviewer;recommendation=result;remarks=note;reviewDate=now;reviewRound=p.reviewRound;}
}
@Entity @Table(name="correction_requests") class CorrectionRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;Long requestedBy;@Enumerated(EnumType.STRING)Role requestingRole;@Column(length=2000)String remarks;Instant requestedAt;boolean resolved;@Column(length=2000)String applicantResponse;Instant resubmittedAt;int reviewRound;
 protected CorrectionRequest(){} CorrectionRequest(PermitRecord p,AppUser actor,String note,Instant now){applicationId=p.id;requestedBy=actor.id;requestingRole=actor.role;remarks=note;requestedAt=now;reviewRound=p.reviewRound;}
}
@Entity @Table(name="payment_records") class PaymentRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;BigDecimal feeAmount;@Enumerated(EnumType.STRING)PaymentStatus paymentStatus;String paymentReference;Long recordedBy;Instant recordedAt;
 protected PaymentRecord(){}
}
@Entity @Table(name="inspections") class InspectionRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;Long inspectorId;String inspectionType;LocalDate scheduledDate;LocalDate conductedDate;@Column(length=2000)String observations;@Enumerated(EnumType.STRING)InspectionResult result;boolean finalInspection;Instant createdAt;Instant updatedAt;
 protected InspectionRecord(){}
}
@Entity @Table(name="workflow_history") class WorkflowHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;Long applicationId;@Enumerated(EnumType.STRING)PermitStatus previousStatus;@Enumerated(EnumType.STRING)PermitStatus newStatus;String action;@Column(length=2000)String remarks;Long actorId;String actorName;String actorEmail;@Enumerated(EnumType.STRING)Role actorRole;Instant occurredAt;
 protected WorkflowHistory(){} WorkflowHistory(PermitRecord p,PermitStatus from,String action,String note,AppUser actor,Instant now){applicationId=p.id;previousStatus=from;newStatus=p.status;this.action=action;remarks=note;actorId=actor.id;actorName=actor.fullName;actorEmail=actor.email;actorRole=actor.role;occurredAt=now;}
}
