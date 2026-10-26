package edu.kle.permits;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class PermitDtos {
 private PermitDtos(){}
 public record ApplicationInput(
  @NotBlank @Size(max=150) String projectTitle,
  @NotBlank @Size(max=80) String permitType,
  @NotBlank @Size(max=300) String propertyAddress,
  @NotBlank @Size(max=2000) String constructionDescription,
  @NotBlank @Size(max=150) String propertyOwnerName,
  @NotBlank @Size(max=150) String contractorArchitectName,
  @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal estimatedProjectCost,
  @NotNull LocalDate proposedStartDate,
  @NotBlank @Size(max=2000) String supportingDocuments){}
 public record Resubmit(@NotBlank @Size(max=2000) String response){}
 public record DocumentDecision(boolean complete,@Size(max=2000) String remarks){}
 public record AssignmentInput(@NotNull AssignmentType type,@NotNull Long assigneeId){}
 public record ReviewInput(@NotNull ReviewRecommendation recommendation,@Size(max=2000) String remarks){}
 public record DecisionInput(@NotNull String decision,@Size(max=2000) String remarks){}
 public record PaymentInput(@NotNull @DecimalMin("0.00") BigDecimal feeAmount,@NotNull PaymentStatus status,@Size(max=100) String paymentReference){}
 public record InspectionInput(@NotBlank @Size(max=100) String inspectionType,@NotNull LocalDate scheduledDate,boolean finalInspection){}
 public record InspectionResultInput(@NotNull LocalDate conductedDate,@NotBlank @Size(max=2000) String observations,@NotNull InspectionResult result){}
 public record Person(Long id,String name,String email){}
 public record AssignmentView(Long id,AssignmentType type,Person assignee,Person assignedBy,Instant assignedAt,boolean active){}
 public record ReviewView(Long id,ReviewType type,Person reviewer,ReviewRecommendation recommendation,String remarks,Instant reviewDate,int round){}
 public record CorrectionView(Long id,Person requestedBy,Role requestingRole,String remarks,Instant requestedAt,boolean resolved,String applicantResponse,Instant resubmittedAt,int round){}
 public record PaymentView(BigDecimal feeAmount,PaymentStatus status,String reference,Person recordedBy,Instant recordedAt){}
 public record InspectionView(Long id,Person inspector,String type,LocalDate scheduledDate,LocalDate conductedDate,String observations,InspectionResult result,boolean finalInspection,Instant createdAt){}
 public record HistoryView(Long id,PermitStatus previousStatus,PermitStatus newStatus,String action,String remarks,Long actorId,String actorName,String actorEmail,Role actorRole,Instant occurredAt){}
 public record ApplicationSummary(Long id,String permitNumber,String projectTitle,String permitType,String applicantName,List<String> assignedOfficerNames,PermitStatus status,int reviewRound,Instant updatedAt){}
 public record ApplicationView(Long id,String permitNumber,Person applicant,String projectTitle,String permitType,String propertyAddress,String constructionDescription,String propertyOwnerName,String contractorArchitectName,BigDecimal estimatedProjectCost,LocalDate proposedStartDate,String supportingDocuments,PermitStatus status,int reviewRound,Instant createdAt,Instant updatedAt,List<AssignmentView> assignments,List<ReviewView> reviews,List<CorrectionView> corrections,PaymentView payment,List<InspectionView> inspections,List<HistoryView> history,Set<String> availableActions){}
 public record Dashboard(Map<String,Long> counts,List<ApplicationSummary> applications){}
}
