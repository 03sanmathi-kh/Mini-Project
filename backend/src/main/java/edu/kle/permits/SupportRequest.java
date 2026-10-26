package edu.kle.permits;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="support_requests")
public class SupportRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true,length=30) public String ticketNumber;
 @Column(nullable=false,length=100) public String requesterName;
 @Column(nullable=false,length=254) public String personalContactEmail;
 @Column(length=254) public String permitworksEmail;
 @Column(length=30) public String contactPhone;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) public SupportCategory category;
 @Column(nullable=false,length=160) public String subject;
 @Column(nullable=false,length=4000) public String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public SupportStatus status=SupportStatus.OPEN;
 @Column(length=4000) public String adminNotes;
 @Column(length=4000) public String userVisibleResponse;
 public Long handledByUserId;
 @Column(nullable=false,length=64) public String requestIpHash;
 @Column(nullable=false) public Instant createdAt;
 @Column(nullable=false) public Instant updatedAt;
 public Instant resolvedAt;
 protected SupportRequest(){}
}
