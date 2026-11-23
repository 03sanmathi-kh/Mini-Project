package edu.kle.permits;

import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class AuditService {
 private final AuditRecords records; private final Clock clock;
 public AuditService(AuditRecords records,Clock clock){this.records=records;this.clock=clock;}
 @Transactional public void record(AppUser actor,String action,String entity,Long id,String details){
  AuditRecord r=new AuditRecord();r.actorId=actor.id;r.action=action;r.entityType=entity;r.entityId=id;
  r.occurredAt=clock.instant();r.eventDate=LocalDate.ofInstant(r.occurredAt,ZoneOffset.UTC);
  r.details=details==null?"":details;records.save(r);
 }
}
