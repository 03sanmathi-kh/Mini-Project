package edu.kle.permits;

import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Call inside the account-creation transaction, before taking user/request row locks. */
@Component public class RegistrationIdentityGuard {
 private final RegistrationMutexes mutex;private final Users users;private final RegistrationRequests requests;
 public RegistrationIdentityGuard(RegistrationMutexes m,Users u,RegistrationRequests r){mutex=m;users=u;requests=r;}
 public void lock(){mutex.acquire();}
 public void available(String email,Long excludedUser,Long excludedRequest){
  boolean used=users.findAll().stream().anyMatch(u->!Objects.equals(u.id,excludedUser)&&(email.equalsIgnoreCase(u.email)||u.personalContactEmail!=null&&email.equalsIgnoreCase(u.personalContactEmail)));
  boolean reserved=requests.findByActiveEmail(email).filter(r->!Objects.equals(r.id,excludedRequest)&&!(excludedUser!=null&&Objects.equals(r.userId,excludedUser))).isPresent();
  if(used||reserved)throw new ResponseStatusException(HttpStatus.CONFLICT,"These details are unavailable for a new request. Check an earlier request or contact Help.");
 }
}
