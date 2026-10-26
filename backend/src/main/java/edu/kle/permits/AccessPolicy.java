package edu.kle.permits;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
/** Integration helper: E2/E4 must call this against IDs read from the database, never client claims. */
@Component("accessPolicy")
public class AccessPolicy {
 public boolean owns(Authentication auth,Long ownerId){return auth!=null&&auth.isAuthenticated()&&ownerId!=null&&auth.getName().equals(ownerId.toString());}
 public boolean assigned(Authentication auth,Long assigneeId){return owns(auth,assigneeId);}
}
