package edu.kle.permits;

import jakarta.persistence.*;
import java.time.Instant;

enum RegistrationStatus { PENDING,APPROVED,REJECTED,ACTIVATED }
@Entity @Table(name="registration_requests")
public class RegistrationRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String fullName;String personalEmail;String activeEmail;String phone;
 @Enumerated(EnumType.STRING) Role requestedRole;
 @Enumerated(EnumType.STRING) Role approvedRole;
 @Column(length=2000) String description;
 @Enumerated(EnumType.STRING) RegistrationStatus status=RegistrationStatus.PENDING;
 String trackingHash;String activationHash;
 Instant submittedAt;Instant reviewedAt;Long reviewedBy;
 @Column(length=500) String rejectionReason;
 Long userId;Instant activationExpiresAt;Instant activatedAt;
}
