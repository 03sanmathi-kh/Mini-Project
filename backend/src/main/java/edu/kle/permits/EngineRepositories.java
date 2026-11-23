package edu.kle.permits;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;

interface AssignmentMutexes extends JpaRepository<AssignmentMutex,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from AssignmentMutex m where m.id=1") AssignmentMutex acquire();
}
interface RoutingProfiles extends JpaRepository<OfficerRouting,Long> {}
interface UnassignedWorks extends JpaRepository<UnassignedWork,Long> {
 Optional<UnassignedWork> findByApplicationIdAndAssignmentType(Long app,AssignmentType type);
 List<UnassignedWork> findByResolvedFalseOrderByCreatedAt();
}
interface Notifications extends JpaRepository<Notification,Long> {
 List<Notification> findTop100ByRecipientIdOrderByCreatedAtDesc(Long id);
 Optional<Notification> findByIdAndRecipientId(Long id,Long recipient);
}
interface AuditRecords extends org.springframework.data.repository.Repository<AuditRecord,Long> {
 AuditRecord save(AuditRecord record);
 Page<AuditRecord> findAllByOrderByOccurredAtDesc(Pageable page);
}
interface DocumentVersions extends JpaRepository<DocumentVersion,Long> {
 List<DocumentVersion> findByApplicationIdOrderByUploadedAt(Long app);
 Optional<DocumentVersion> findFirstByApplicationIdAndDocumentNameOrderByVersionNoDesc(Long app,String name);
}
