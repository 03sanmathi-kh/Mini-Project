package edu.kle.permits;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class SupportDtos {
 private SupportDtos(){}
 public record Create(
  @NotBlank @Size(max=100) String fullName,
  @NotBlank @Size(max=254) @Email String personalContactEmail,
  @Size(max=254) @Pattern(regexp="^$|(?i)^[a-z0-9][a-z0-9._-]{0,63}@permitworks\\.com$",message="must be a valid @permitworks.com email") String permitworksEmail,
  @Size(max=30) @Pattern(regexp="^$|^[+0-9][0-9 ()-]{6,29}$",message="must be a valid phone number") String contactPhone,
  @NotNull SupportCategory category,
  @NotBlank @Size(max=160) String subject,
  @NotBlank @Size(max=4000) String description){}
 public record Created(String message,String ticketNumber){}
 public record StatusCheck(@NotBlank @Size(max=30) String ticketNumber,@NotBlank @Size(max=254) @Email String personalContactEmail){}
 public record PublicStatus(String ticketNumber,SupportCategory category,SupportStatus status,Instant submittedAt,Instant updatedAt,String userVisibleResponse){}
 public record Summary(Long id,String ticketNumber,String requesterName,String personalContactEmail,String permitworksEmail,SupportCategory category,String subject,SupportStatus status,Instant createdAt){}
 public record MatchedAccount(Long id,String fullName,String permitworksEmail,String personalContactEmail,Role role,boolean enabled,boolean locked){}
 public record AdminView(Long id,String ticketNumber,String requesterName,String personalContactEmail,String permitworksEmail,String contactPhone,SupportCategory category,String subject,String description,SupportStatus status,String adminNotes,String userVisibleResponse,Instant createdAt,Instant updatedAt,Instant resolvedAt,MatchedAccount matchedAccount){}
 public record Update(@NotNull SupportStatus status,@Size(max=4000) String adminNotes,@Size(max=4000) String userVisibleResponse){}
}
