package edu.kle.permits;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/permits")
public class PermitController {
 private final PermitService service;public PermitController(PermitService service){this.service=service;}
 @GetMapping("/dashboard") public PermitDtos.Dashboard dashboard(HttpServletRequest r){return service.dashboard(actor(r));}
 @GetMapping("/{id}") public PermitDtos.ApplicationView get(@PathVariable Long id,HttpServletRequest r){return service.get(id,actor(r));}
 @PostMapping @PreAuthorize("hasRole('APPLICANT')") public PermitDtos.ApplicationView create(@Valid @RequestBody PermitDtos.ApplicationInput d,HttpServletRequest r){return service.create(d,actor(r));}
 @PatchMapping("/{id}") @PreAuthorize("hasRole('APPLICANT')") public PermitDtos.ApplicationView edit(@PathVariable Long id,@Valid @RequestBody PermitDtos.ApplicationInput d,HttpServletRequest r){return service.edit(id,d,actor(r));}
 @PostMapping("/{id}/submit") @PreAuthorize("hasRole('APPLICANT')") public PermitDtos.ApplicationView submit(@PathVariable Long id,HttpServletRequest r){return service.submit(id,actor(r));}
 @PostMapping("/{id}/resubmit") @PreAuthorize("hasRole('APPLICANT')") public PermitDtos.ApplicationView resubmit(@PathVariable Long id,@Valid @RequestBody PermitDtos.Resubmit d,HttpServletRequest r){return service.resubmit(id,d,actor(r));}
 @PostMapping("/{id}/documents") @PreAuthorize("hasRole('DEPARTMENT_OFFICER')") public PermitDtos.ApplicationView documents(@PathVariable Long id,@Valid @RequestBody PermitDtos.DocumentDecision d,HttpServletRequest r){return service.verifyDocuments(id,d,actor(r));}
 @PostMapping("/{id}/assignments") @PreAuthorize("hasRole('DEPARTMENT_OFFICER')") public PermitDtos.ApplicationView assign(@PathVariable Long id,@Valid @RequestBody PermitDtos.AssignmentInput d,HttpServletRequest r){return service.assign(id,d,actor(r));}
 @GetMapping("/officers/{role}") @PreAuthorize("hasAnyRole('DEPARTMENT_OFFICER','ADMIN')") public List<Dtos.UserView> officers(@PathVariable Role role,HttpServletRequest r){return service.officers(role,actor(r));}
 @PostMapping("/{id}/technical-review") @PreAuthorize("hasRole('PERMIT_REVIEWER')") public PermitDtos.ApplicationView technical(@PathVariable Long id,@Valid @RequestBody PermitDtos.ReviewInput d,HttpServletRequest r){return service.review(id,ReviewType.TECHNICAL,d,actor(r));}
 @PostMapping("/{id}/zoning-review") @PreAuthorize("hasRole('ZONING_OFFICER')") public PermitDtos.ApplicationView zoning(@PathVariable Long id,@Valid @RequestBody PermitDtos.ReviewInput d,HttpServletRequest r){return service.review(id,ReviewType.ZONING,d,actor(r));}
 @PostMapping("/{id}/decision") @PreAuthorize("hasRole('SENIOR_DEPARTMENT_OFFICER')") public PermitDtos.ApplicationView decision(@PathVariable Long id,@Valid @RequestBody PermitDtos.DecisionInput d,HttpServletRequest r){return service.decide(id,d,actor(r));}
 @PostMapping("/{id}/payment") @PreAuthorize("hasRole('FINANCE_OFFICER')") public PermitDtos.ApplicationView payment(@PathVariable Long id,@Valid @RequestBody PermitDtos.PaymentInput d,HttpServletRequest r){return service.payment(id,d,actor(r));}
 @PostMapping("/{id}/inspections") @PreAuthorize("hasRole('BUILDING_INSPECTOR')") public PermitDtos.ApplicationView schedule(@PathVariable Long id,@Valid @RequestBody PermitDtos.InspectionInput d,HttpServletRequest r){return service.schedule(id,d,actor(r));}
 @PostMapping("/{id}/inspections/{inspectionId}/result") @PreAuthorize("hasRole('BUILDING_INSPECTOR')") public PermitDtos.ApplicationView result(@PathVariable Long id,@PathVariable Long inspectionId,@Valid @RequestBody PermitDtos.InspectionResultInput d,HttpServletRequest r){return service.recordInspection(id,inspectionId,d,actor(r));}
 @PostMapping("/{id}/close") @PreAuthorize("hasRole('BUILDING_INSPECTOR')") public PermitDtos.ApplicationView close(@PathVariable Long id,HttpServletRequest r){return service.close(id,actor(r));}
 private AppUser actor(HttpServletRequest r){return (AppUser)r.getAttribute("currentUser");}
}
