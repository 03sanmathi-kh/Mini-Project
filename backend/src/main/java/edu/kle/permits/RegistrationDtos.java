package edu.kle.permits;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Locale;

public final class RegistrationDtos {
 private RegistrationDtos(){}
 static String trim(String value){return value==null?null:value.trim();}
 static String email(String value){return value==null?null:value.trim().toLowerCase(Locale.ROOT);}
 /** Public applicant registration. The registration password is used only for status checks. */
 public record Submit(
  @NotBlank @Size(max=100) @Pattern(regexp="^[\\p{L}\\p{M}][\\p{L}\\p{M} .’'\\-]{0,99}$",message="Enter a name using letters, spaces, apostrophes, periods or hyphens.") String fullName,
  @NotBlank @Email @Size(max=254) String personalEmail,
  @ValidPassword String registrationPassword,
  @NotBlank @Size(max=1024) String confirmRegistrationPassword,
  @NotBlank @Pattern(regexp="^[0-9]{10}$",message="Phone number must be exactly 10 digits.") String phone,
  @NotNull Role requestedRole,
  @Size(max=2000) String description){
  public Submit {fullName=trim(fullName);personalEmail=email(personalEmail);phone=trim(phone);description=trim(description);}
  @Override public String toString(){return "Submit[personalEmail="+personalEmail+", requestedRole="+requestedRole+"]";}
 }
 /** Status check with exactly the two registration credentials. */
 public record Check(@NotBlank @Email @Size(max=254) String personalEmail,@NotBlank @Size(max=1024) String registrationPassword){
  public Check{personalEmail=email(personalEmail);}
  @Override public String toString(){return "Check[personalEmail="+personalEmail+"]";}
 }
 /** Admin approval: creates a separate PermitWorks login account with Admin-chosen credentials. */
 public record Approve(
  @NotNull Role approvedRole,
  @NotBlank @Size(max=254) @Pattern(regexp="(?i)^[a-z0-9][a-z0-9._-]{0,63}@permitworks\\.com$",message="Login email must be a valid @permitworks.com address.") String loginEmail,
  @ValidPassword String loginPassword,
  @NotBlank @Size(max=1024) String confirmLoginPassword,
  @AssertTrue(message="Confirm approval and the approved role.") boolean confirm,@Size(max=500)String adminRemarks){
  public Approve(Role role,String email,String password,String confirmation,boolean confirm){this(role,email,password,confirmation,confirm,null);}
  public Approve{loginEmail=email(loginEmail);}
  @Override public String toString(){return "Approve[approvedRole="+approvedRole+", loginEmail="+loginEmail+"]";}
 }
 public record Reject(@NotBlank @Size(max=500) String reason,@AssertTrue(message="Confirm rejection.") boolean confirm){public Reject{reason=trim(reason);}}
 public record Submitted(String message){}
 /** Public status deliberately excludes dedicated login credentials and credential hashes. */
 public record PublicStatus(RegistrationStatus status,String message,String rejectionReason){
  @Override public String toString(){return "PublicStatus[status="+status+"]";}
 }
 public record AdminView(Long id,String fullName,String personalEmail,String phone,Role requestedRole,Role approvedRole,String description,RegistrationStatus status,Instant submittedAt,Instant reviewedAt,String rejectionReason,String loginEmail){}
}
