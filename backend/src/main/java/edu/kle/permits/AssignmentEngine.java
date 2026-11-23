package edu.kle.permits;

import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Serializes allocation decisions across application rows, including concurrent least-load requests. */
@Service public class AssignmentEngine {
 private final AssignmentMutexes mutex;private final PermitRecords permits;private final Assignments assignments;
 private final Users users;private final RoutingProfiles profiles;private final UnassignedWorks queue;
 private final NotificationService notifications;private final AuditService audit;private final Clock clock;
 public AssignmentEngine(AssignmentMutexes m,PermitRecords p,Assignments a,Users u,RoutingProfiles r,UnassignedWorks q,NotificationService n,AuditService au,Clock c){mutex=m;permits=p;assignments=a;users=u;profiles=r;queue=q;notifications=n;audit=au;clock=c;}
 public record Outcome(Long officerId,boolean assigned,String reason){}
 public record Override(Long officerId,boolean confirm,String reason){}
 private static void denied(){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only Department Officer or Admin may manage routing.");}
 static void manager(AppUser actor){if(actor.role!=Role.DEPARTMENT_OFFICER&&actor.role!=Role.ADMIN)denied();}
 private PermitRecord begin(Long id,AssignmentType type,AppUser actor){
  manager(actor);if(type==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Assignment type is required.");
  mutex.acquire();PermitRecord p=permits.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found."));
  boolean allowed=type==AssignmentType.BUILDING_INSPECTOR?
   Set.of(PermitStatus.APPROVED,PermitStatus.PAYMENT_PENDING,PermitStatus.PERMIT_ISSUED,PermitStatus.INSPECTION_FAILED,PermitStatus.INSPECTION_PASSED).contains(p.status):
   Set.of(PermitStatus.DOCUMENT_VERIFICATION,PermitStatus.ASSIGNED,PermitStatus.TECHNICAL_REVIEW,PermitStatus.RESUBMITTED).contains(p.status);
  if(!allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Assignment is not available at this workflow stage.");
  return p;
 }
 private boolean matches(OfficerRouting r,PermitRecord p,AssignmentType type){return r.assignmentType==type&&(r.permitType.equals("*")||r.permitType.equalsIgnoreCase(p.permitType))&&(r.zone.equals("*")||r.zone.equalsIgnoreCase(p.zone));}
 private long load(Long officer){return assignments.findByAssigneeIdAndActiveTrue(officer).stream().filter(a->!a.completed).count();}
 @Transactional public Outcome automatic(Long app,AssignmentType type,AppUser actor){
  PermitRecord p=begin(app,type,actor);
  var existing=assignments.findFirstByApplicationIdAndAssignmentTypeAndActiveTrue(app,type);
  if(existing.isPresent())return new Outcome(existing.get().assigneeId,true,"Already assigned; use confirmed override to change officer.");
  Map<Long,OfficerRouting> eligible=new TreeMap<>();
  profiles.findAll().stream().filter(r->matches(r,p,type)).sorted(Comparator.comparing((OfficerRouting r)->r.permitType.equals("*")?1:0).thenComparing(r->r.zone.equals("*")?1:0).thenComparing(r->r.id)).forEach(r->{
   users.findById(r.officerId).filter(u->u.enabled&&!u.locked&&u.role.name().equals(type.name())).ifPresent(u->eligible.putIfAbsent(u.id,r));
  });
  record Candidate(Long id,long load){}
  PriorityQueue<Candidate> heap=new PriorityQueue<>(Comparator.comparingLong(Candidate::load).thenComparing(Candidate::id));
  eligible.keySet().forEach(id->heap.add(new Candidate(id,load(id))));
  if(heap.isEmpty()){
   UnassignedWork w=queue.findByApplicationIdAndAssignmentType(app,type).orElseGet(UnassignedWork::new);
   w.applicationId=app;w.assignmentType=type;w.reason="No enabled, unlocked officer with matching role/type/zone/stage routing profile.";w.createdAt=clock.instant();w.resolved=false;queue.save(w);
   notifications.department(app,"UNASSIGNED",p.permitNumber+": no eligible "+type);
   audit.record(actor,"ASSIGNMENT_UNAVAILABLE","permit",app,w.reason);return new Outcome(null,false,w.reason);
  }
  Candidate chosen=heap.remove();save(p,type,chosen.id(),null,null,eligible.get(chosen.id()).slaHours,actor);
  return new Outcome(chosen.id(),true,"Least-loaded eligible officer; ties use lowest officer ID.");
 }
 @Transactional public Outcome manual(Long app,AssignmentType type,Override input,AppUser actor){
  PermitRecord p=begin(app,type,actor);
  if(input==null||input.officerId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Officer is required.");
  AppUser officer=users.findById(input.officerId()).filter(u->u.enabled&&!u.locked&&u.role.name().equals(type.name())).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Officer must be enabled, unlocked and have the matching role."));
  var previous=assignments.findFirstByApplicationIdAndAssignmentTypeAndActiveTrue(app,type);
  if(previous.isPresent()&&(!input.confirm()||input.reason()==null||input.reason().isBlank()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Reassignment requires explicit confirmation and a reason.");
  if(input.reason()!=null&&input.reason().length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Override reason is too long.");
  Integer hours=profiles.findAll().stream().filter(r->r.officerId.equals(officer.id)&&matches(r,p,type)).map(r->r.slaHours).filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
  save(p,type,officer.id,previous.map(a->a.assigneeId).orElse(null),input.reason(),hours,actor);
  return new Outcome(officer.id,true,"Manual assignment recorded.");
 }
 private void save(PermitRecord p,AssignmentType type,Long officer,Long previous,String reason,Integer hours,AppUser actor){
  assignments.deactivate(p.id,type);
  ApplicationAssignment a=new ApplicationAssignment(p.id,type,officer,actor.id,clock.instant());a.previousAssigneeId=previous;a.overrideReason=reason;a.dueAt=hours==null?null:clock.instant().plus(Duration.ofHours(hours));assignments.save(a);
  queue.findByApplicationIdAndAssignmentType(p.id,type).ifPresent(w->w.resolved=true);
  audit.record(actor,previous==null?"OFFICER_ASSIGNED":"ASSIGNMENT_OVERRIDE","permit",p.id,"previous="+previous+"; new="+officer+"; type="+type+"; reason="+(reason==null?"":reason));
  notifications.send(officer,p.id,"OFFICER_ASSIGNED",p.permitNumber+": assigned "+type);
 }
 @Transactional public void complete(Long app,AssignmentType type){assignments.findByApplicationIdOrderByAssignedAt(app).stream().filter(a->a.active&&a.assignmentType==type).forEach(a->a.completed=true);}
 public record ProfileInput(Long officerId,String permitType,String zone,AssignmentType type,Integer slaHours){}
 @Transactional public OfficerRouting profile(ProfileInput d,AppUser actor){
  manager(actor);mutex.acquire();
  if(d==null||d.officerId()==null||d.type()==null||d.zone()==null||d.zone().isBlank()||d.zone().length()>80||d.permitType()==null||d.permitType().isBlank()||d.permitType().length()>80||d.slaHours()!=null&&(d.slaHours()<1||d.slaHours()>8760))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valid officer, type, zone, stage and optional SLA 1–8760 hours are required.");
  users.findById(d.officerId()).filter(u->u.enabled&&u.role.name().equals(d.type().name())).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Officer role does not match routing stage."));
  OfficerRouting r=profiles.findAll().stream().filter(x->x.officerId.equals(d.officerId())&&x.permitType.equalsIgnoreCase(d.permitType().trim())&&x.zone.equalsIgnoreCase(d.zone().trim())&&x.assignmentType==d.type()).findFirst().orElseGet(OfficerRouting::new);
  r.officerId=d.officerId();r.permitType=d.permitType().trim();r.zone=d.zone().trim();r.assignmentType=d.type();r.slaHours=d.slaHours();profiles.save(r);audit.record(actor,"ROUTING_PROFILE_SAVED","officer",d.officerId(),"type="+r.permitType+"; zone="+r.zone+"; stage="+r.assignmentType+"; slaHours="+r.slaHours);return r;
 }
 public record Work(Long applicationId,String type,Long officerId,Instant assignedAt,Instant dueAt,boolean overdue){}
 public record OfficerLoad(Long officerId,String name,String role,long active,long overdue){}
 public record Report(List<OfficerLoad> officers,List<Work> assignments,List<UnassignedWork> unassigned,long pending,long overdue,long completed,Double averageProcessingHours,long deadlinesUnconfigured){}
 @Transactional(readOnly=true) public Report report(AppUser actor){return report(actor,null,null,null);}
 @Transactional(readOnly=true) public Report report(AppUser actor,LocalDate from,LocalDate to,String permitType){
  manager(actor);if(from!=null&&to!=null&&from.isAfter(to))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Report start date must not follow end date.");
  var all=permits.findAll().stream().filter(p->(from==null||!p.createdAt.atZone(ZoneOffset.UTC).toLocalDate().isBefore(from))&&(to==null||!p.createdAt.atZone(ZoneOffset.UTC).toLocalDate().isAfter(to))&&(permitType==null||permitType.isBlank()||p.permitType.equalsIgnoreCase(permitType))).toList();
  Set<Long> selected=new HashSet<>();all.forEach(p->selected.add(p.id));
  Instant now=clock.instant();List<Work> work=assignments.findAll().stream().filter(a->a.active&&!a.completed&&selected.contains(a.applicationId)).map(a->new Work(a.applicationId,a.assignmentType.name(),a.assigneeId,a.assignedAt,a.dueAt,a.dueAt!=null&&!now.isBefore(a.dueAt))).toList();
  List<OfficerLoad> loads=users.findAll().stream().filter(u->Set.of(Role.PERMIT_REVIEWER,Role.ZONING_OFFICER,Role.BUILDING_INSPECTOR).contains(u.role)).map(u->new OfficerLoad(u.id,u.fullName,u.role.name(),work.stream().filter(a->a.officerId.equals(u.id)).count(),work.stream().filter(a->a.officerId.equals(u.id)&&a.overdue).count())).toList();
  var finished=all.stream().filter(p->p.status==PermitStatus.PROJECT_CLOSED||p.status==PermitStatus.REJECTED).toList();
  Double avg=finished.isEmpty()?null:finished.stream().mapToDouble(p->Duration.between(p.createdAt,p.updatedAt).toMillis()/3600000.0).average().orElse(0);
  return new Report(loads,work,queue.findByResolvedFalseOrderByCreatedAt().stream().filter(q->selected.contains(q.applicationId)).toList(),all.size()-finished.size(),work.stream().filter(Work::overdue).count(),finished.size(),avg,work.stream().filter(a->a.dueAt==null).count());
 }
}
