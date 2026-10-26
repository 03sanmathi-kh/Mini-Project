package edu.kle.permits;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="app_users")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String fullName;
 @Column(nullable=false,unique=true) public String email;
 public String personalContactEmail;
 @Column(nullable=false) public String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role;
 public boolean enabled=true;
 public boolean locked=false;
 public int failedAttempts=0;
 public Instant createdAt=Instant.now();
 public Instant updatedAt=Instant.now();
 protected AppUser(){}
 public AppUser(String name,String email,String personalEmail,String hash,Role role){this.fullName=name;this.email=email;this.personalContactEmail=personalEmail;this.passwordHash=hash;this.role=role;}
 public AppUser(String name,String email,String hash,Role role){this(name,email,null,hash,role);}
}
