package edu.kle.permits;

import jakarta.persistence.*;
import java.time.Instant;

enum RegistrationStatus { PENDING,APPROVED,REJECTED,ACTIVATED }
@Entity @Table(name="registration_requests")
public class RegistrationRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="registration_id") Long id;
 @Column(name="name") String fullName;String personalEmail;String activeEmail;@Column(name="phone_number") String phone;
 @Enumerated(EnumType.STRING) @Column(name="requested_user_type") Role requestedRole;
 @Enumerated(EnumType.STRING) Role approvedRole;
 @Column(length=2000) String description;
 @Enumerated(EnumType.STRING) @Column(name="registration_status") RegistrationStatus status=RegistrationStatus.PENDING;
 String trackingHash;String activationHash;
 /** BCrypt hash of the applicant-chosen registration password. Used only for public status checks, never for login. */
 @Column(length=100) String registrationPasswordHash;
 /** Sealed credential delivery (see CredentialVault). None of these columns hold a readable password. */
 @Column(length=1000) String credentialPublicKey;
 @Column(length=4000) String credentialPrivateKeyEnc;
 @Column(length=64) String credentialKdfSalt;
 @Column(length=64) String credentialIv;
 @Column(length=1000) String sealedLoginPassword;
 @Column(name="created_at") Instant submittedAt;Instant reviewedAt;Long reviewedBy;
 @Column(length=500,name="admin_remarks") String rejectionReason;
 Long userId;Instant activationExpiresAt;Instant activatedAt;
}
