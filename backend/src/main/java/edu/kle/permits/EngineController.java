package edu.kle.permits;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api") public class EngineController {
 private final AssignmentEngine routing;private final PermitService permits;private final RoutingProfiles profiles;
 private final Notifications notifications;private final AuditRecords audit;private final Clock clock;
 public EngineController(AssignmentEngine r,PermitService p,RoutingProfiles rp,Notifications n,AuditRecords a,Clock c){routing=r;permits=p;profiles=rp;notifications=n;audit=a;clock=c;}
 @PostMapping("/permits/{id}/auto-assignment/{type}") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
 public AssignmentEngine.Outcome automatic(@PathVariable Long id,@PathVariable AssignmentType type,HttpServletRequest r){return permits.automatic(id,type,actor(r));}
 @PostMapping("/permits/{id}/override/{type}") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
 public PermitDtos.ApplicationView override(@PathVariable Long id,@PathVariable AssignmentType type,@RequestBody AssignmentEngine.Override input,HttpServletRequest r){return permits.assign(id,new PermitDtos.AssignmentInput(type,input.officerId(),input.confirm(),input.reason()),actor(r));}
 @GetMapping("/reports/workload") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
 public AssignmentEngine.Report report(@RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,@RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,@RequestParam(required=false) String permitType,HttpServletRequest r){return routing.report(actor(r),from,to,permitType);}
 @GetMapping("/routing/profiles") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
 public List<OfficerRouting> profiles(){return profiles.findAll();}
 @PostMapping("/routing/profiles") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
 public OfficerRouting profile(@RequestBody AssignmentEngine.ProfileInput input,HttpServletRequest r){return routing.profile(input,actor(r));}
 @GetMapping("/notifications") public List<Notification> notifications(HttpServletRequest r){return notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(actor(r).id);}
 @PostMapping("/notifications/{id}/read") @Transactional
 public Map<String,String> read(@PathVariable Long id,HttpServletRequest r){Notification n=notifications.findByIdAndRecipientId(id,actor(r).id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found."));if(n.readAt==null)n.readAt=clock.instant();return Map.of("message","Notification marked read");}
 @GetMapping("/audit") @PreAuthorize("hasRole('ADMIN')")
 public Page<AuditRecord> audit(@RequestParam(defaultValue="0") int page){return audit.findAllByOrderByOccurredAtDesc(PageRequest.of(Math.max(0,page),50));}
 private AppUser actor(HttpServletRequest r){return (AppUser)r.getAttribute("currentUser");}
}
