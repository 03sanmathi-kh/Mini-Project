package edu.kle.permits;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Locale;

public final class RegistrationDtos {
 private RegistrationDtos(){}
 static String trim(String value){return value==null?null:value.trim();}
 static String email(String value){return value==null?null:value.trim().toLowerCase(Locale.ROOT);}
 public record Submit(
  @NotBlank @Size(max=100) @Pattern(regexp="^[\\p{L}\\p{M}][\\p{L}\\p{M} .’'\\-]{0,99}$",message="Enter a name using letters, spaces, apostrophes, periods or hyphens.") String fullName,
  @NotBlank @Email @Size(max=254) String personalEmail,
  @NotBlank @Size(max=30) @Pattern(regexp="^\\+?[0-9][0-9 ()-]{6,29}$",message="Enter a valid phone number with 7-15 digits.") String phone,
  @NotNull Role requestedRole,
  @Size(max=2000) String description){
  public Submit {fullName=trim(fullName);personalEmail=email(personalEmail);phone=trim(phone);description=trim(description);}
 }
 public record Check(@NotBlank @Email @Size(max=254) String personalEmail,@NotBlank @Pattern(regexp="PWREG-[A-Za-z0-9_-]{32}") String trackingCode){public Check{personalEmail=email(personalEmail);trackingCode=trim(trackingCode);}}
 public record Approve(@NotNull Role approvedRole,@AssertTrue(message="Confirm approval and the approved role.") boolean confirm){}
 public record Reject(@NotBlank @Size(max=500) String reason,@AssertTrue(message="Confirm rejection.") boolean confirm){public Reject{reason=trim(reason);}}
 public record Activate(@NotBlank @Email @Size(max=254) String personalEmail,@NotBlank @Pattern(regexp="PWREG-[A-Za-z0-9_-]{32}") String trackingCode,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String activationSecret,@ValidPassword String newPassword,@NotBlank @Size(max=1024) String confirmPassword){public Activate{personalEmail=email(personalEmail);trackingCode=trim(trackingCode);activationSecret=trim(activationSecret);}}
 public record Submitted(String message,String trackingCode,String activationSecret){}
 public record PublicStatus(RegistrationStatus status,String message,String loginEmail,Role approvedRole,String rejectionReason,Instant activationExpiresAt){}
 public record Activated(String message,String loginEmail){}
 public record AdminView(Long id,String fullName,String personalEmail,String phone,Role requestedRole,Role approvedRole,String description,RegistrationStatus status,Instant submittedAt,Instant reviewedAt,String rejectionReason,String loginEmail,Instant activationExpiresAt){}
}
