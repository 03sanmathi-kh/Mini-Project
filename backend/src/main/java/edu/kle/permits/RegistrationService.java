package edu.kle.permits;

import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service public class RegistrationService {
 private final RegistrationRequests requests;private final Users users;private final RegistrationIdentityGuard identity;
 private final PasswordEncoder encoder;private final Clock clock;private final AuditService audit;private final Events events;
 private final SecureRandom random=new SecureRandom();
 public RegistrationService(RegistrationRequests r,Users u,RegistrationIdentityGuard i,PasswordEncoder p,Clock c,AuditService a,Events e){requests=r;users=u;identity=i;encoder=p;clock=c;audit=a;events=e;}
 @Transactional public RegistrationDtos.Submitted submit(RegistrationDtos.Submit input){
  identity.lock();String email=RegistrationDtos.email(input.personalEmail());identity.available(email,null,null);safeText(input.description());
  String digits=input.phone().replaceAll("[^0-9]","");if(digits.length()<7||digits.length()>15)throw bad("Phone number must contain 7-15 digits.");
  String tracking="PWREG-"+token(24),secret=token(32);RegistrationRequest r=new RegistrationRequest();
  r.fullName=input.fullName().trim();r.personalEmail=r.activeEmail=email;r.phone=(input.phone().startsWith("+")?"+":"")+digits;r.requestedRole=input.requestedRole();r.description=RegistrationDtos.trim(input.description());
  r.trackingHash=RegistrationRateLimiter.digest(tracking);r.activationHash=RegistrationRateLimiter.digest(secret);r.submittedAt=clock.instant();requests.save(r);
  events.save(new SecurityEvent(null,"REGISTRATION_SUBMITTED",null));
  return new RegistrationDtos.Submitted("Your account request has been sent to the PermitWorks Administrator for approval.",tracking,secret);
 }
 @Transactional(readOnly=true) public RegistrationDtos.PublicStatus status(RegistrationDtos.Check input){
  RegistrationRequest r=verified(input.personalEmail(),input.trackingCode());
  String login=r.userId==null?null:users.findById(r.userId).orElseThrow().email;
  String message=switch(r.status){case PENDING->"Your registration request is under review.";case APPROVED->"Your registration has been approved.";case REJECTED->"Your registration request was rejected.";case ACTIVATED->"Your account is active. You can now log in.";};
  return new RegistrationDtos.PublicStatus(r.status,message,login,r.approvedRole,r.status==RegistrationStatus.REJECTED?r.rejectionReason:null,r.activationExpiresAt);
 }
 @Transactional(readOnly=true) public Page<RegistrationDtos.AdminView> list(RegistrationStatus status,int page){Pageable p=PageRequest.of(Math.max(page,0),20,Sort.by(Sort.Direction.DESC,"submittedAt"));return (status==null?requests.findAll(p):requests.findByStatus(status,p)).map(this::view);}
 @Transactional public RegistrationDtos.AdminView approve(Long id,RegistrationDtos.Approve input,AppUser actor){
  admin(actor);identity.lock();RegistrationRequest r=locked(id);pending(r);if(!input.confirm()||input.approvedRole()==null)throw bad("Confirm approval and select the approved role.");
  identity.available(r.personalEmail,null,r.id);
  String login;do{login="reg-"+HexFormat.of().formatHex(bytes(12))+"@permitworks.com";}while(users.existsByEmailIgnoreCase(login));
  AppUser user=new AppUser(r.fullName,login,r.personalEmail,encoder.encode(token(32)),input.approvedRole());user.enabled=false;user.activationPending=true;users.save(user);
  r.userId=user.id;r.approvedRole=input.approvedRole();r.status=RegistrationStatus.APPROVED;r.reviewedAt=clock.instant();r.reviewedBy=actor.id;r.activationExpiresAt=r.reviewedAt.plus(Duration.ofDays(7));
  audit.record(actor,"REGISTRATION_APPROVED","registration",r.id,"approvedRole="+r.approvedRole+"; user="+user.id);events.save(new SecurityEvent(actor.id,"REGISTRATION_APPROVED",user.id));return view(r);
 }
 @Transactional public RegistrationDtos.AdminView reject(Long id,RegistrationDtos.Reject input,AppUser actor){
  admin(actor);identity.lock();RegistrationRequest r=locked(id);pending(r);if(!input.confirm()||input.reason()==null||input.reason().isBlank())throw bad("Confirm rejection and enter a reason.");safeText(input.reason());
  r.status=RegistrationStatus.REJECTED;r.rejectionReason=input.reason().trim();r.reviewedAt=clock.instant();r.reviewedBy=actor.id;r.activationHash=null;r.activeEmail=null;
  audit.record(actor,"REGISTRATION_REJECTED","registration",r.id,"Request rejected; user-visible reason recorded.");events.save(new SecurityEvent(actor.id,"REGISTRATION_REJECTED",null));return view(r);
 }
 @Transactional public RegistrationDtos.Activated activate(RegistrationDtos.Activate input){
  identity.lock();RegistrationRequest r=verified(input.personalEmail(),input.trackingCode());r=locked(r.id);Instant now=clock.instant();
  boolean valid=r.activationHash!=null&&MessageDigest.isEqual(r.activationHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),RegistrationRateLimiter.digest(input.activationSecret()).getBytes(java.nio.charset.StandardCharsets.US_ASCII));
  if(r.status!=RegistrationStatus.APPROVED||!valid||r.activationExpiresAt==null||!now.isBefore(r.activationExpiresAt))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verification failed or activation is unavailable. Check your details or contact Help.");
  Passwords.require(input.newPassword());if(!input.newPassword().equals(input.confirmPassword()))throw bad("Passwords do not match.");
  AppUser user=users.lockById(r.userId).orElseThrow();if(!user.activationPending)throw new ResponseStatusException(HttpStatus.CONFLICT,"Activation is no longer available.");
  user.passwordHash=encoder.encode(input.newPassword());user.activationPending=false;user.enabled=true;user.locked=false;user.failedAttempts=0;user.updatedAt=now;
  r.activationHash=null;r.status=RegistrationStatus.ACTIVATED;r.activatedAt=now;
  audit.record(user,"REGISTRATION_ACTIVATED","registration",r.id,"Single-use private activation completed.");events.save(new SecurityEvent(user.id,"REGISTRATION_ACTIVATED",user.id));
  return new RegistrationDtos.Activated("Your account is active. You can now log in.",user.email);
 }
 private RegistrationRequest verified(String email,String tracking){return requests.findByTrackingHashAndPersonalEmail(RegistrationRateLimiter.digest(tracking.trim()),RegistrationDtos.email(email)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration details could not be verified."));}
 private RegistrationRequest locked(Long id){return requests.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration request not found."));}
 private void pending(RegistrationRequest r){if(r.status!=RegistrationStatus.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only pending requests can be reviewed.");}
 private void admin(AppUser actor){if(actor==null||actor.role!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only Admin can review registration requests.");}
 private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 private void safeText(String text){if(text!=null&&text.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw bad("Control characters are not allowed.");}
 private byte[] bytes(int count){byte[] value=new byte[count];random.nextBytes(value);return value;}
 private String token(int count){return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes(count));}
 private RegistrationDtos.AdminView view(RegistrationRequest r){String login=r.userId==null?null:users.findById(r.userId).orElseThrow().email;return new RegistrationDtos.AdminView(r.id,r.fullName,r.personalEmail,r.phone,r.requestedRole,r.approvedRole,r.description,r.status,r.submittedAt,r.reviewedAt,r.rejectionReason,login,r.activationExpiresAt);}
}
