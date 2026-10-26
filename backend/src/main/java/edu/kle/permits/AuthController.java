package edu.kle.permits;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 final AuthService auth;final boolean secure;
 public AuthController(AuthService auth,@Value("${app.secure-cookie}")boolean secure){this.auth=auth;this.secure=secure;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
 @PostMapping("/login") public ResponseEntity<?> login(@Valid @RequestBody Dtos.Login input){
  var r=auth.login(input);
  if(r.error()!=null)return ResponseEntity.status(401).body(new Dtos.ErrorBody(r.error()));
  return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(r.token(),28800)).body(r.user());
 }
 @GetMapping("/me") public Dtos.UserView me(HttpServletRequest req){return Dtos.UserView.of((AppUser)req.getAttribute("currentUser"));}
 @PostMapping("/activity") public Map<String,String> activity(){return Map.of("status","active");}
 @PostMapping("/logout") public ResponseEntity<?> logout(HttpServletRequest req){AppUser u=(AppUser)req.getAttribute("currentUser");auth.logout((UUID)req.getAttribute("sessionId"),u.id);return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",0)).body(Map.of("message","Logged out"));}
 private String cookie(String value,long age){return ResponseCookie.from("PERMIT_TOKEN",value).httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(age).build().toString();}
}
