package edu.kle.permits;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

/** Resource APIs delegate to the existing transaction, role and ownership checks. */
@RestController @RequestMapping("/api") public class DomainController {
 private final PermitService service;private final DocumentController files;private final Users users;
 DomainController(PermitService s,DocumentController d,Users u){service=s;files=d;users=u;}
 public record PaymentCommand(@NotNull Long applicationId,@Valid @NotNull PermitDtos.PaymentInput payment){}
 public record AssignmentCommand(@NotNull Long applicationId,@Valid @NotNull PermitDtos.AssignmentInput assignment){}
 public record InspectionCommand(@NotNull Long applicationId,@Valid @NotNull PermitDtos.InspectionInput inspection){}
 public record CorrectionCommand(@NotNull Long applicationId,@NotBlank @Size(max=2000)String description){}
 @GetMapping("/applicants") @PreAuthorize("hasRole('ADMIN')") public List<ApprovedErModel.ProfileView> applicants(){return users.findByRoleAndEnabledTrueOrderByFullName(Role.APPLICANT).stream().map(ApprovedErModel.ProfileView::of).toList();}
 @GetMapping("/applicants/{id}") public ApprovedErModel.ProfileView applicant(@PathVariable Long id,HttpServletRequest r){if(actor(r).role!=Role.ADMIN&&!actor(r).id.equals(id))throw missing();return users.findById(id).filter(u->u.role==Role.APPLICANT).map(ApprovedErModel.ProfileView::of).orElseThrow(this::missing);}
 @GetMapping("/officers") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')") public List<ApprovedErModel.ProfileView> officers(){return users.findAll().stream().filter(u->u.role!=Role.APPLICANT&&u.role!=Role.ADMIN).map(ApprovedErModel.ProfileView::of).toList();}
 @GetMapping("/payments") public List<PermitDtos.PaymentEntry> payments(@RequestParam Long applicationId,HttpServletRequest r){return service.get(applicationId,actor(r)).payments();}
 @PostMapping("/payments") public PermitDtos.ApplicationView payment(@Valid @RequestBody PaymentCommand d,HttpServletRequest r){return service.payment(d.applicationId(),d.payment(),actor(r));}
 @GetMapping("/assignments") public List<PermitDtos.AssignmentView> assignments(@RequestParam Long applicationId,HttpServletRequest r){return service.get(applicationId,actor(r)).assignments();}
 @PostMapping("/assignments") public PermitDtos.ApplicationView assign(@Valid @RequestBody AssignmentCommand d,HttpServletRequest r){return service.assign(d.applicationId(),d.assignment(),actor(r));}
 @GetMapping("/inspections") public List<PermitDtos.InspectionView> inspections(@RequestParam Long applicationId,HttpServletRequest r){return service.get(applicationId,actor(r)).inspections();}
 @PostMapping("/inspections") public PermitDtos.ApplicationView inspection(@Valid @RequestBody InspectionCommand d,HttpServletRequest r){return service.schedule(d.applicationId(),d.inspection(),actor(r));}
 @PostMapping("/inspections/{id}/result") public PermitDtos.ApplicationView result(@PathVariable Long id,@RequestParam Long applicationId,@Valid @RequestBody PermitDtos.InspectionResultInput d,HttpServletRequest r){return service.recordInspection(applicationId,id,d,actor(r));}
 @GetMapping("/corrections") public List<PermitDtos.CorrectionView> corrections(@RequestParam Long applicationId,HttpServletRequest r){return service.get(applicationId,actor(r)).corrections();}
 @PostMapping("/corrections") public PermitDtos.ApplicationView correction(@Valid @RequestBody CorrectionCommand d,HttpServletRequest r){AppUser u=actor(r);return switch(u.role){case ADMIN,DEPARTMENT_OFFICER->service.verifyDocuments(d.applicationId(),new PermitDtos.DocumentDecision(false,d.description()),u);case PERMIT_REVIEWER->service.review(d.applicationId(),ReviewType.TECHNICAL,new PermitDtos.ReviewInput(ReviewRecommendation.CORRECTION_REQUIRED,d.description()),u);case ZONING_OFFICER->service.review(d.applicationId(),ReviewType.ZONING,new PermitDtos.ReviewInput(ReviewRecommendation.CORRECTION_REQUIRED,d.description()),u);case SENIOR_DEPARTMENT_OFFICER->service.decide(d.applicationId(),new PermitDtos.DecisionInput("CORRECTION",d.description()),u);default->throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only authorized reviewing officers can request corrections.");};}
 @PostMapping("/corrections/{applicationId}/submit") public PermitDtos.ApplicationView resubmit(@PathVariable Long applicationId,@Valid @RequestBody PermitDtos.Resubmit d,HttpServletRequest r){return service.resubmit(applicationId,d,actor(r));}
 @GetMapping("/documents") public List<DocumentController.Metadata> documents(@RequestParam Long applicationId,HttpServletRequest r){return files.list(applicationId,r);}
 @PostMapping("/documents") public DocumentController.Metadata upload(@RequestParam Long applicationId,@RequestParam("file") MultipartFile file,HttpServletRequest r)throws Exception{return files.upload(applicationId,file,r);}
 @GetMapping("/documents/{id}") public ResponseEntity<byte[]> download(@PathVariable Long id,@RequestParam Long applicationId,HttpServletRequest r)throws Exception{return files.download(applicationId,id,r);}
 private AppUser actor(HttpServletRequest r){return (AppUser)r.getAttribute("currentUser");}
 private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Applicant not found.");}
}
