package edu.kle.permits;

import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class NotificationService {
 private final Notifications notifications;private final Users users;private final Assignments assignments;private final Clock clock;private final SmsProvider sms;
 public NotificationService(Notifications n,Users u,Assignments a,Clock c,SmsProvider s){notifications=n;users=u;assignments=a;clock=c;sms=s;}
 @Transactional public void send(Long recipient,Long app,String event,String text){
  Notification n=new Notification();n.recipientId=recipient;n.applicationId=app;n.event=event;n.message=text;n.createdAt=clock.instant();
  // SMS failure must not prevent in-app delivery. No actual SMS is sent by the local provider.
  try{n.smsStatus=sms.send(recipient,event);}catch(RuntimeException e){n.smsStatus="FAILED";}
  notifications.save(n);
 }
 @Transactional public void department(Long app,String event,String text){users.findByRoleAndEnabledTrueOrderByFullName(Role.DEPARTMENT_OFFICER).forEach(u->send(u.id,app,event,text));}
 @Transactional public void workflow(PermitRecord p,String event){
  Set<Long> recipients=new HashSet<>();recipients.add(p.applicantId);
  assignments.findByApplicationIdOrderByAssignedAt(p.id).stream().filter(a->a.active).forEach(a->recipients.add(a.assigneeId));
  String text=p.permitNumber+": "+event.replace('_',' ');
  recipients.forEach(id->send(id,p.id,event,text));
  if(event.equals("APPLICATION_SUBMITTED")||event.equals("APPLICATION_RESUBMITTED"))department(p.id,event,text);
 }
}
