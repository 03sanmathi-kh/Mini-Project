package edu.kle.permits;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="assignment_mutex") class AssignmentMutex { @Id public Long id; }
@Entity @Table(name="officer_routing") class OfficerRouting {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long officerId; public String permitType; public String zone;
 @Enumerated(EnumType.STRING) public AssignmentType assignmentType; public Integer slaHours;
}
@Entity @Table(name="unassigned_work") class UnassignedWork {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long applicationId; @Enumerated(EnumType.STRING) public AssignmentType assignmentType;
 public String reason; public Instant createdAt; public boolean resolved;
}
@Entity @Table(name="notifications") class Notification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long recipientId; public Long applicationId; public String event; public String message;
 public Instant createdAt; public Instant readAt; public String smsStatus;
}
@Entity @Table(name="audit_log") class AuditRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long actorId; public String action; public LocalDate eventDate; public Instant occurredAt;
 public String entityType; public Long entityId; @Column(length=2000) public String details;
}
@Entity @Table(name="documents") class DocumentVersion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public @Column(name="document_id") Long id;
 public Long applicationId; public String documentName; public int versionNo; @Column(name="mime_type")public String contentType;
 public String contentHash; @Column(name="file_size")public long contentSize; @Column(columnDefinition="text") public String contentBase64;
 @Column(name="applicant_id")public Long uploadedBy; @Column(name="upload_date")public Instant uploadedAt;
 @Column(insertable=false,updatable=false) public String filePath;
}
