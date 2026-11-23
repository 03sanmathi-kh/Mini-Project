package edu.kle.permits;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class AuthService {
 private static final Logger log=LoggerFactory.getLogger(AuthService.class);
 final Users users; final Sessions sessions; final Events events; final PasswordEncoder encoder; final JwtEncoder jwt; final Clock clock;
 final String dummyHash;
 public AuthService(Users users,Sessions sessions,Events events,PasswordEncoder encoder,JwtEncoder jwt,Clock clock){this.users=users;this.sessions=sessions;this.events=events;this.encoder=encoder;this.jwt=jwt;this.clock=clock;dummyHash=encoder.encode(UUID.randomUUID().toString());}
 public record LoginResult(String token,Dtos.UserView user,String error){}
 @Transactional
 public LoginResult login(Dtos.Login input){
  String email=PermitEmails.normalizeFull(input.email());
  var found=users.lockByEmail(email);
  if(found.isEmpty()){encoder.matches(input.password(),dummyHash);return new LoginResult(null,null,"Invalid email or password.");}
  AppUser u=found.get();
  if(u.locked)return new LoginResult(null,null,"Account locked. Contact your administrator to unlock it.");
  if(u.activationPending)return new LoginResult(null,null,"Account activation is required before signing in.");
  if(!u.enabled)return new LoginResult(null,null,"Account disabled. Contact your administrator.");
  if(!Passwords.valid(input.password())||!encoder.matches(input.password(),u.passwordHash)){
   u.failedAttempts++; u.locked=u.failedAttempts>=5;u.updatedAt=clock.instant();
   events.save(new SecurityEvent(u.id,u.locked?"ACCOUNT_LOCKED":"LOGIN_FAILED",u.id));
   return new LoginResult(null,null,u.locked?"Account locked after 5 failed attempts. Contact your administrator.":"Invalid email or password.");
  }
  u.failedAttempts=0;u.updatedAt=clock.instant();Instant now=clock.instant();
  AuthSession s=sessions.save(new AuthSession(u.id,now));
  JwtClaimsSet claims=JwtClaimsSet.builder().issuer("permit-engine1").subject(u.id.toString()).claim("email",u.email).id(s.id.toString()).issuedAt(now).expiresAt(s.expiresAt).build();
  String token=jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
  events.save(new SecurityEvent(u.id,"LOGIN_SUCCEEDED",u.id));
  return new LoginResult(token,Dtos.UserView.of(u),null);
 }
 public record Verified(AppUser user,AuthSession session){}
 @Transactional
 public Verified verify(Jwt token){
  Long userId;UUID sid;
  try{userId=Long.valueOf(token.getSubject());sid=UUID.fromString(token.getId());}catch(Exception e){return null;}
  // Always lock user before session, matching admin operations and avoiding lock inversion.
  AppUser u=users.lockById(userId).orElse(null);
  if(u==null||!u.enabled||u.locked||u.activationPending)return null;
  AuthSession s=sessions.lockById(sid).orElse(null); Instant now=clock.instant();
  if(s==null||!s.userId.equals(u.id)||s.revoked)return null;
  if(!now.isBefore(s.expiresAt)||!now.isBefore(s.lastActivity.plusSeconds(AuthSession.IDLE_TIMEOUT_SECONDS))){s.revoked=true;log.info("Session rejected: absolute lifetime expired.");return null;}
  return new Verified(u,s);
 }
 @Transactional public void activity(UUID sid,Long userId){sessions.lockById(sid).filter(s->s.userId.equals(userId)&&!s.revoked).ifPresent(s->{Instant now=clock.instant();if(now.isBefore(s.expiresAt)&&now.isBefore(s.lastActivity.plusSeconds(AuthSession.IDLE_TIMEOUT_SECONDS)))s.lastActivity=now;});}
 @Transactional public void logout(UUID sid,Long userId){sessions.findById(sid).ifPresent(s->{s.revoked=true;});events.save(new SecurityEvent(userId,"LOGOUT",userId));}
}
