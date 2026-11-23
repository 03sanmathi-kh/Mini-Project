package edu.kle.permits;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

/** Public registration endpoints. Registration credentials never authenticate into PermitWorks. */
@RestController @RequestMapping("/api/registrations")
public class RegistrationController {
 private final RegistrationService service;private final RegistrationRateLimiter limiter;
 public RegistrationController(RegistrationService s,RegistrationRateLimiter l){service=s;limiter=l;}
 @GetMapping("/roles") public List<Role> roles(){return RegistrationService.REGISTRABLE_ROLES;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public RegistrationDtos.Submitted submit(@Valid @RequestBody RegistrationDtos.Submit d,HttpServletRequest r){limiter.check("submit",r.getRemoteAddr());return service.submit(d);}
 @PostMapping("/status") public RegistrationDtos.PublicStatus status(@Valid @RequestBody RegistrationDtos.Check d,HttpServletRequest r){limiter.check("status",r.getRemoteAddr());limiter.check("status-email",d.personalEmail());return service.status(d);}
}

@RestController @RequestMapping("/api/admin/registrations") @PreAuthorize("hasRole('ADMIN')")
class AdminRegistrationController {
 private final RegistrationService service;
 AdminRegistrationController(RegistrationService s){service=s;}
 @GetMapping public Page<RegistrationDtos.AdminView> list(@RequestParam(required=false)RegistrationStatus status,@RequestParam(defaultValue="0")int page){return service.list(status,page);}
 @GetMapping("/roles") public List<Role> roles(){return RegistrationService.REGISTRABLE_ROLES;}
 @PostMapping("/{id}/approve") public RegistrationDtos.AdminView approve(@PathVariable Long id,@Valid @RequestBody RegistrationDtos.Approve d,HttpServletRequest r){return service.approve(id,d,(AppUser)r.getAttribute("currentUser"));}
 @PostMapping("/{id}/reject") public RegistrationDtos.AdminView reject(@PathVariable Long id,@Valid @RequestBody RegistrationDtos.Reject d,HttpServletRequest r){return service.reject(id,d,(AppUser)r.getAttribute("currentUser"));}
}
