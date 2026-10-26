package edu.kle.permits;

import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class Bootstrap {
 @Bean ApplicationRunner initialAdmin(Users users,PasswordEncoder encoder,@Value("${app.admin-local-part}")String localPart,@Value("${app.admin-password}")String password){return args->{
  if(users.count()==0){
   if(!Passwords.valid(password))throw new IllegalArgumentException("ADMIN_PASSWORD must be 8–72 UTF-8 bytes.");
   users.save(new AppUser("System Administrator",PermitEmails.fromLocalPart(localPart),null,encoder.encode(password),Role.ADMIN));
  }
 };}
}
