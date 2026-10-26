package edu.kle.permits;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface PermitRecords extends JpaRepository<PermitRecord,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PermitRecord p where p.id=:id") Optional<PermitRecord> lockById(@Param("id")Long id);
 List<PermitRecord> findByApplicantIdOrderByUpdatedAtDesc(Long id);
 List<PermitRecord> findAllByOrderByUpdatedAtDesc();
}
interface ApplicationVersions extends JpaRepository<ApplicationVersion,Long>{List<ApplicationVersion> findByApplicationIdOrderByReviewRound(Long id);}
interface Assignments extends JpaRepository<ApplicationAssignment,Long>{
 Optional<ApplicationAssignment> findFirstByApplicationIdAndAssignmentTypeAndActiveTrue(Long app,AssignmentType type);
 List<ApplicationAssignment> findByApplicationIdOrderByAssignedAt(Long app);
 List<ApplicationAssignment> findByAssigneeIdAndActiveTrue(Long user);
 @Modifying @Query("update ApplicationAssignment a set a.active=false where a.applicationId=:app and a.assignmentType=:type and a.active=true") int deactivate(@Param("app")Long app,@Param("type")AssignmentType type);
}
interface Reviews extends JpaRepository<ApplicationReview,Long>{List<ApplicationReview> findByApplicationIdOrderByReviewDate(Long app);boolean existsByApplicationIdAndReviewTypeAndReviewRoundAndRecommendation(Long app,ReviewType type,int round,ReviewRecommendation result);}
interface Corrections extends JpaRepository<CorrectionRequest,Long>{List<CorrectionRequest> findByApplicationIdOrderByRequestedAt(Long app);List<CorrectionRequest> findByApplicationIdAndResolvedFalse(Long app);}
interface Payments extends JpaRepository<PaymentRecord,Long>{Optional<PaymentRecord> findByApplicationId(Long app);}
interface Inspections extends JpaRepository<InspectionRecord,Long>{List<InspectionRecord> findByApplicationIdOrderByCreatedAt(Long app);Optional<InspectionRecord> findByIdAndInspectorId(Long id,Long inspector);}
interface WorkflowHistories extends JpaRepository<WorkflowHistory,Long>{List<WorkflowHistory> findByApplicationIdOrderByOccurredAt(Long app);}
