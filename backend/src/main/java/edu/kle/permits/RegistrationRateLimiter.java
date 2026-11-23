package edu.kle.permits;

import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service public class RegistrationRateLimiter {
 private final RegistrationMutexes mutex;private final RegistrationLimits limits;private final Clock clock;private final String salt;
 public RegistrationRateLimiter(RegistrationMutexes m,RegistrationLimits l,Clock c,@Value("${app.jwt-secret}")String s){mutex=m;limits=l;clock=c;salt=s;}
 @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=ResponseStatusException.class)
 public void check(String operation,String address){
  mutex.acquire();String key=digest(salt+"|"+operation+"|"+(address==null?"unknown":address));Instant now=clock.instant();
  int max=operation.equals("submit")?5:operation.equals("activate")?10:30;
  long seconds=operation.equals("submit")?3600:operation.equals("activate")?900:600;
  RegistrationLimit l=limits.findById(key).orElseGet(RegistrationLimit::new);
  if(l.id==null||!now.isBefore(l.windowStart.plusSeconds(seconds))){l.id=key;l.windowStart=now;l.attempts=1;}else{if(l.attempts>=max)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many requests. Please wait before trying again.");l.attempts++;}
  limits.save(l);
 }
 static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Digest unavailable",e);}}
}
