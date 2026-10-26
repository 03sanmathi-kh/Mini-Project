package edu.kle.permits;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class AdminService {
 final Users users;final Sessions sessions;final Events events;final PasswordEncoder encoder;
 public AdminService(Users u,Sessions s,Events e,PasswordEncoder p){users=u;sessions=s;events=e;encoder=p;}
 @Transactional public Dtos.UserView create(Dtos.CreateUser d,Long actor){
  String email=PermitEmails.fromLocalPart(d.localPart());
  if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email address already exists.");
  Passwords.require(d.password());
  String personalEmail=PermitEmails.normalizePersonal(d.personalContactEmail());
  AppUser u=users.save(new AppUser(d.fullName().trim(),email,personalEmail,encoder.encode(d.password()),d.role()));
  events.save(new SecurityEvent(actor,"USER_CREATED",u.id));return Dtos.UserView.of(u);
 }
 @Transactional public Dtos.UserView update(Long id,Dtos.UpdateUser d,Long actor){
  AppUser u=users.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found."));
  if(id.equals(actor)&&(!d.enabled()||d.role()!=Role.ADMIN))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cannot disable yourself or remove your own Admin role.");
  boolean revoke=u.role!=d.role()||u.enabled!=d.enabled();
  u.fullName=d.fullName().trim();u.personalContactEmail=d.personalContactEmail()==null||d.personalContactEmail().isBlank()?null:PermitEmails.normalizePersonal(d.personalContactEmail());u.role=d.role();u.enabled=d.enabled();u.updatedAt=Instant.now();
  if(revoke)sessions.revokeForUser(id);
  events.save(new SecurityEvent(actor,"USER_UPDATED",id));return Dtos.UserView.of(u);
 }
 @Transactional public Dtos.UserView unlock(Long id,Long actor){
  AppUser u=users.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found."));
  u.locked=false;u.failedAttempts=0;u.updatedAt=Instant.now();sessions.revokeForUser(id);
  events.save(new SecurityEvent(actor,"ACCOUNT_UNLOCKED",id));return Dtos.UserView.of(u);
 }
 @Transactional public Dtos.UserView enable(Long id,Long actor){
  AppUser u=users.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found."));
  u.enabled=true;u.updatedAt=Instant.now();sessions.revokeForUser(id);
  events.save(new SecurityEvent(actor,"ACCOUNT_ENABLED",id));return Dtos.UserView.of(u);
 }
}
