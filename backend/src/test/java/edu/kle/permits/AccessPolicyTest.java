package edu.kle.permits;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class AccessPolicyTest {
 @Test void ownershipAndAssignmentRequireMatchingTrustedId(){
  var auth=new UsernamePasswordAuthenticationToken("12",null,List.of());var policy=new AccessPolicy();
  assertTrue(policy.owns(auth,12L));assertFalse(policy.owns(auth,13L));assertFalse(policy.owns(null,12L));assertFalse(policy.assigned(auth,null));
 }
}
