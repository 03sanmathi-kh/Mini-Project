package edu.kle.permits;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Two separate credential systems:
 * registration credentials (personal email + applicant-chosen password) are used only for status checks;
 * application login credentials (dedicated @permitworks.com email + Admin-chosen password) are created by Admin on approval.
 */
@Service public class RegistrationService {
 public static final List<Role> REGISTRABLE_ROLES=Arrays.stream(Role.values()).filter(r->r!=Role.ADMIN).toList();
 static final String INVALID_CREDENTIALS="Registration details could not be verified. Check your personal email and registration password.";
 private final RegistrationRequests requests;private final Users users;private final RegistrationIdentityGuard identity;
 private final PasswordEncoder encoder;private final Clock clock;private final AuditService audit;private final Events events;
 private final String dummyHash;
 public RegistrationService(RegistrationRequests r,Users u,RegistrationIdentityGuard i,PasswordEncoder p,Clock c,AuditService a,Events e){requests=r;users=u;identity=i;encoder=p;clock=c;audit=a;events=e;dummyHash=p.encode(UUID.randomUUID().toString());}

 @Transactional public RegistrationDtos.Submitted submit(RegistrationDtos.Submit input){
  registrable(input.requestedRole());
  if(!input.phone().matches("[0-9]{10}"))throw bad("phone: Phone number must be exactly 10 digits.");
  strong(input.registrationPassword(),"registrationPassword");
  if(!input.registrationPassword().equals(input.confirmRegistrationPassword()))throw bad("confirmRegistrationPassword: Passwords do not match.");
  safeText(input.description());
  identity.lock();String email=RegistrationDtos.email(input.personalEmail());identity.available(email,null,null);
  RegistrationRequest r=new RegistrationRequest();
  r.fullName=input.fullName().trim();r.personalEmail=r.activeEmail=email;r.phone=input.phone();r.requestedRole=input.requestedRole();r.description=RegistrationDtos.trim(input.description());
  r.registrationPasswordHash=encoder.encode(input.registrationPassword());r.submittedAt=clock.instant();requests.save(r);
  events.save(new SecurityEvent(null,"REGISTRATION_SUBMITTED",null));
  return new RegistrationDtos.Submitted("Registration submitted successfully. It is awaiting Admin approval. Use your personal email and registration password to check its status.");
 }

 @Transactional(readOnly=true) public RegistrationDtos.PublicStatus status(RegistrationDtos.Check input){
  List<RegistrationRequest> candidates=requests.findByPersonalEmailOrderBySubmittedAtDesc(RegistrationDtos.email(input.personalEmail())).stream().filter(r->r.registrationPasswordHash!=null).toList();
  RegistrationRequest match=null;
  for(RegistrationRequest r:candidates)if(encoder.matches(input.registrationPassword(),r.registrationPasswordHash)){match=r;break;}
  if(candidates.isEmpty())encoder.matches(input.registrationPassword(),dummyHash); // keep timing similar for unknown emails
  if(match==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,INVALID_CREDENTIALS);
  return switch(match.status){
   case PENDING->new RegistrationDtos.PublicStatus(RegistrationStatus.PENDING,"Registration awaiting Admin approval.",null);
   case APPROVED,ACTIVATED->new RegistrationDtos.PublicStatus(RegistrationStatus.APPROVED,"Registration approved. Contact Admin for your PermitWorks login credentials.",null);
   case REJECTED->new RegistrationDtos.PublicStatus(RegistrationStatus.REJECTED,"Registration rejected. Contact Admin for assistance.",match.rejectionReason);
  };
 }

 @Transactional(readOnly=true) public Page<RegistrationDtos.AdminView> list(RegistrationStatus status,int page){Pageable p=PageRequest.of(Math.max(page,0),20,Sort.by(Sort.Direction.DESC,"submittedAt"));return (status==null?requests.findAll(p):requests.findByStatus(status,p)).map(this::view);}

 /** Creates the dedicated PermitWorks login account and approves the request in one transaction. */
 @Transactional public RegistrationDtos.AdminView approve(Long id,RegistrationDtos.Approve input,AppUser actor){
  admin(actor);
  if(!input.confirm())throw bad("Confirm approval and select the approved role.");
  registrable(input.approvedRole());
  String loginEmail=PermitEmails.normalizeFull(input.loginEmail());
  strong(input.loginPassword(),"loginPassword");
  if(!input.loginPassword().equals(input.confirmLoginPassword()))throw bad("confirmLoginPassword: Login passwords do not match.");
  identity.lock();RegistrationRequest r=locked(id);pending(r);
  if(loginEmail.equalsIgnoreCase(r.personalEmail))throw bad("loginEmail: The PermitWorks login email must be different from the applicant's personal email.");
  if(r.registrationPasswordHash!=null&&encoder.matches(input.loginPassword(),r.registrationPasswordHash))throw bad("loginPassword: Choose a login password that is different from the applicant's registration password.");
  if(users.existsByEmailIgnoreCase(loginEmail))throw new ResponseStatusException(HttpStatus.CONFLICT,"loginEmail: This PermitWorks login email is already in use.");
  AppUser user=new AppUser(r.fullName,loginEmail,null,encoder.encode(input.loginPassword()),input.approvedRole());
  user.enabled=true;user.activationPending=false;user.locked=false;user.failedAttempts=0;
  users.saveAndFlush(user);
  Instant now=clock.instant();
  r.userId=user.id;r.approvedRole=input.approvedRole();r.status=RegistrationStatus.APPROVED;r.reviewedAt=now;r.reviewedBy=actor.id;r.activationHash=null;
  requests.saveAndFlush(r);
  audit.record(actor,"REGISTRATION_APPROVED","registration",r.id,"approvedRole="+r.approvedRole+"; user="+user.id);
  events.save(new SecurityEvent(actor.id,"USER_CREATED",user.id));events.save(new SecurityEvent(actor.id,"REGISTRATION_APPROVED",user.id));
  return view(r);
 }

 @Transactional public RegistrationDtos.AdminView reject(Long id,RegistrationDtos.Reject input,AppUser actor){
  admin(actor);if(!input.confirm()||input.reason()==null||input.reason().isBlank())throw bad("Confirm rejection and enter a reason.");safeText(input.reason());
  identity.lock();RegistrationRequest r=locked(id);pending(r);
  r.status=RegistrationStatus.REJECTED;r.rejectionReason=input.reason().trim();r.reviewedAt=clock.instant();r.reviewedBy=actor.id;r.activationHash=null;r.activeEmail=null;
  audit.record(actor,"REGISTRATION_REJECTED","registration",r.id,"Request rejected; user-visible reason recorded.");events.save(new SecurityEvent(actor.id,"REGISTRATION_REJECTED",null));return view(r);
 }

 /** Password strength: 8-72 UTF-8 bytes and at least three of lowercase, uppercase, digit, symbol. */
 static boolean strongPassword(String p){
  if(!Passwords.valid(p))return false;
  int classes=(p.chars().anyMatch(Character::isLowerCase)?1:0)+(p.chars().anyMatch(Character::isUpperCase)?1:0)+(p.chars().anyMatch(Character::isDigit)?1:0)+(p.chars().anyMatch(c->!Character.isLetterOrDigit(c)&&!Character.isWhitespace(c))?1:0);
  return classes>=3;
 }
 private static void strong(String p,String field){if(!strongPassword(p))throw bad(field+": Use 8-72 characters with at least three of: lowercase, uppercase, number, symbol.");}
 private static void registrable(Role role){if(role==null||!REGISTRABLE_ROLES.contains(role))throw bad("Requested role is not available for registration.");}
 private RegistrationRequest locked(Long id){return requests.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration request not found."));}
 private void pending(RegistrationRequest r){if(r.status!=RegistrationStatus.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only pending requests can be reviewed.");}
 private void admin(AppUser actor){if(actor==null||actor.role!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only Admin can review registration requests.");}
 private static ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 private void safeText(String text){if(text!=null&&text.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw bad("Control characters are not allowed.");}
 private RegistrationDtos.AdminView view(RegistrationRequest r){String login=r.userId==null?null:users.findById(r.userId).map(u->u.email).orElse(null);return new RegistrationDtos.AdminView(r.id,r.fullName,r.personalEmail,r.phone,r.requestedRole,r.approvedRole,r.description,r.status,r.submittedAt,r.reviewedAt,r.rejectionReason,login);}
}
