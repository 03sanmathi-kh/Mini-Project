package edu.kle.permits;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportService {
 private static final Duration COOLDOWN=Duration.ofSeconds(30);
 final SupportRequests requests;final Users users;final Events events;final AdminService admin;final Clock clock;
 public SupportService(SupportRequests requests,Users users,Events events,AdminService admin,Clock clock){this.requests=requests;this.users=users;this.events=events;this.admin=admin;this.clock=clock;}
 @Transactional public SupportDtos.Created create(SupportDtos.Create input,String remoteAddress){
  clean(input.fullName());clean(input.subject());clean(input.description());clean(input.contactPhone());
  String personal=PermitEmails.normalizePersonal(input.personalContactEmail());
  String permitworks=input.permitworksEmail()==null||input.permitworksEmail().isBlank()?null:PermitEmails.normalizeFull(input.permitworksEmail());
  Instant now=clock.instant();String ipHash=hash(remoteAddress==null?"unknown":remoteAddress);
  if(requests.existsByRequestIpHashAndCreatedAtAfter(ipHash,now.minus(COOLDOWN)))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait before submitting another support request.");
  SupportRequest request=new SupportRequest();request.ticketNumber=String.format("PW-HELP-%d-%05d",Year.from(now.atZone(ZoneOffset.UTC)).getValue(),requests.nextTicketSequence());request.requesterName=input.fullName().trim();request.personalContactEmail=personal;request.permitworksEmail=permitworks;request.contactPhone=blank(input.contactPhone());request.category=input.category();request.subject=input.subject().trim();request.description=input.description().trim();request.requestIpHash=ipHash;request.createdAt=request.updatedAt=now;
  requests.save(request);events.save(new SecurityEvent(null,"SUPPORT_REQUEST_CREATED",null));
  return new SupportDtos.Created("Your support request has been submitted.",request.ticketNumber);
 }
 @Transactional(readOnly=true) public SupportDtos.PublicStatus status(SupportDtos.StatusCheck input){
  String email=PermitEmails.normalizePersonal(input.personalContactEmail());String ticket=input.ticketNumber().trim().toUpperCase(Locale.ROOT);
  SupportRequest s=requests.findByTicketNumberAndPersonalContactEmailIgnoreCase(ticket,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"The ticket details could not be found."));
  return new SupportDtos.PublicStatus(s.ticketNumber,s.category,s.status,s.createdAt,s.updatedAt,s.userVisibleResponse);
 }
 @Transactional(readOnly=true) public Page<SupportDtos.Summary> list(String query,SupportStatus status,SupportCategory category,int page){return requests.search(blank(query),status,category,PageRequest.of(Math.max(page,0),20)).map(this::summary);}
 @Transactional(readOnly=true) public long openCount(){return requests.countByStatusIn(List.of(SupportStatus.OPEN,SupportStatus.IN_PROGRESS));}
 @Transactional(readOnly=true) public SupportDtos.AdminView get(Long id){return adminView(request(id));}
 @Transactional public SupportDtos.AdminView update(Long id,SupportDtos.Update input,Long actor){
  clean(input.adminNotes());clean(input.userVisibleResponse());SupportRequest s=request(id);SupportStatus previous=s.status;s.status=input.status();s.adminNotes=blank(input.adminNotes());s.userVisibleResponse=blank(input.userVisibleResponse());s.handledByUserId=actor;s.updatedAt=clock.instant();s.resolvedAt=Set.of(SupportStatus.RESOLVED,SupportStatus.CLOSED).contains(s.status)?s.updatedAt:null;
  if(previous!=s.status)events.save(new SecurityEvent(actor,s.status==SupportStatus.RESOLVED?"SUPPORT_REQUEST_RESOLVED":s.status==SupportStatus.CLOSED?"SUPPORT_REQUEST_CLOSED":"SUPPORT_REQUEST_STATUS_CHANGED",actor));
  return adminView(s);
 }
 @Transactional public SupportDtos.AdminView unlock(Long id,Long actor){SupportRequest s=request(id);AppUser account=matched(s);admin.unlock(account.id,actor);events.save(new SecurityEvent(actor,"SUPPORT_ACCOUNT_UNLOCKED",account.id));s.handledByUserId=actor;s.updatedAt=clock.instant();return adminView(s);}
 @Transactional public SupportDtos.AdminView enable(Long id,Long actor){SupportRequest s=request(id);AppUser account=matched(s);admin.enable(account.id,actor);events.save(new SecurityEvent(actor,"SUPPORT_ACCOUNT_ENABLED",account.id));s.handledByUserId=actor;s.updatedAt=clock.instant();return adminView(s);}
 private SupportRequest request(Long id){return requests.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Support request not found."));}
 private AppUser matched(SupportRequest s){if(s.permitworksEmail==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"No PermitWorks account is associated with this request.");return users.findByEmail(s.permitworksEmail).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"No PermitWorks account is associated with this request."));}
 private SupportDtos.Summary summary(SupportRequest s){return new SupportDtos.Summary(s.id,s.ticketNumber,s.requesterName,s.personalContactEmail,s.permitworksEmail,s.category,s.subject,s.status,s.createdAt);}
 private SupportDtos.AdminView adminView(SupportRequest s){AppUser u=s.permitworksEmail==null?null:users.findByEmail(s.permitworksEmail).orElse(null);SupportDtos.MatchedAccount match=u==null?null:new SupportDtos.MatchedAccount(u.id,u.fullName,u.email,u.personalContactEmail,u.role,u.enabled,u.locked);return new SupportDtos.AdminView(s.id,s.ticketNumber,s.requesterName,s.personalContactEmail,s.permitworksEmail,s.contactPhone,s.category,s.subject,s.description,s.status,s.adminNotes,s.userVisibleResponse,s.createdAt,s.updatedAt,s.resolvedAt,match);}
 private static String blank(String value){return value==null||value.isBlank()?null:value.trim();}
 private static void clean(String value){if(value==null)return;String lower=value.toLowerCase(Locale.ROOT);if(value.indexOf('\0')>=0||value.contains("<")||value.contains(">")||lower.contains("javascript:")||lower.contains("data:text/html"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"HTML, scripts and executable content are not allowed.");}
 private static String hash(String value){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return java.util.HexFormat.of().formatHex(digest);}catch(Exception e){throw new IllegalStateException(e);}}
}
