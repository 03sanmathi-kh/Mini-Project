package edu.kle.permits;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="auth_sessions")
public class AuthSession {
 public static final long ABSOLUTE_TIMEOUT_SECONDS=8*60*60L;
 @Id public UUID id;
 @Column(nullable=false) public Long userId;
 public Instant createdAt;
 public Instant expiresAt;
 @Column(nullable=false) public Instant lastActivity;
 public static final long IDLE_TIMEOUT_SECONDS=30*60L;
 public boolean revoked;
 protected AuthSession(){}
 public AuthSession(Long user, Instant now){id=UUID.randomUUID();userId=user;createdAt=now;lastActivity=now;expiresAt=now.plusSeconds(ABSOLUTE_TIMEOUT_SECONDS);}
}
