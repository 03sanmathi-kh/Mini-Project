package edu.kle.permits;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="auth_sessions")
public class AuthSession {
 @Id public UUID id;
 @Column(nullable=false) public Long userId;
 public Instant createdAt;
 public Instant lastActivity;
 public Instant expiresAt;
 public boolean revoked;
 protected AuthSession(){}
 public AuthSession(Long user, Instant now){id=UUID.randomUUID();userId=user;createdAt=now;lastActivity=now;expiresAt=now.plusSeconds(28800);}
}
