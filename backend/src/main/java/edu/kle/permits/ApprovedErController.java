package edu.kle.permits;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") public class ApprovedErController {
 private final Users users;private final ApprovedErService service;
 ApprovedErController(Users users,ApprovedErService service){this.users=users;this.service=service;}
 @GetMapping("/profile") public ApprovedErModel.ProfileView profile(HttpServletRequest r){return ApprovedErModel.ProfileView.of(actor(r));}
 @PatchMapping("/profile") @Transactional public ApprovedErModel.ProfileView update(@Valid @RequestBody ApprovedErModel.ProfileInput d,HttpServletRequest r){AppUser u=users.lockById(actor(r).id).orElseThrow();u.phoneNo=clean(d.phoneNo());u.address=clean(d.address());users.saveAndFlush(u);return ApprovedErModel.ProfileView.of(u);}
 @GetMapping("/properties") public List<ApprovedErModel.PropertyView> properties(HttpServletRequest r){return service.owned(actor(r));}
 @PostMapping("/properties") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public ApprovedErModel.PropertyView property(@Valid @RequestBody ApprovedErModel.PropertyCreate d,HttpServletRequest r){return service.create(d,actor(r));}
 private AppUser actor(HttpServletRequest r){return (AppUser)r.getAttribute("currentUser");}
 private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
