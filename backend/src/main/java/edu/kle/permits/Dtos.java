package edu.kle.permits;

import jakarta.validation.constraints.*;
import java.util.*;
public final class Dtos {
 public record Login(@NotBlank @Size(max=254) @Pattern(regexp="(?i)^[a-z0-9][a-z0-9._-]{0,63}@permitworks\\.com$") String email,@ValidPassword String password){}
 public record CreateUser(@NotBlank @Size(max=100) String fullName,@NotBlank @Pattern(regexp="^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$") String localPart,@NotBlank @Size(max=254) @Email String personalContactEmail,@ValidPassword String password,@NotNull Role role){}
 public record UpdateUser(@NotBlank @Size(max=100) String fullName,@Size(max=254) @Email String personalContactEmail,@NotNull Role role,@NotNull Boolean enabled){}
 public record UserView(Long id,String fullName,String email,String personalContactEmail,Role role,boolean enabled,boolean locked,int failedAttempts,Set<String> permissions){
  public static UserView of(AppUser u){return new UserView(u.id,u.fullName,u.email,u.personalContactEmail,u.role,u.enabled,u.locked,u.failedAttempts,u.role.permissions);}
 }
 public record ErrorBody(String message){}
}
