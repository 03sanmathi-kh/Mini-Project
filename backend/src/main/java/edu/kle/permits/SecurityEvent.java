package edu.kle.permits;

import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="security_events")
public class SecurityEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long actorId;
 public String action;
 public Long targetId;
 public Instant occurredAt;
 public LocalDate eventDate;
 protected SecurityEvent(){}
 public SecurityEvent(Long actor,String action,Long target){actorId=actor;this.action=action;targetId=target;occurredAt=Instant.now();eventDate=LocalDate.ofInstant(occurredAt,ZoneOffset.UTC);}
}
