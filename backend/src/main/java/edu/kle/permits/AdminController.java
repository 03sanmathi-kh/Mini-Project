package edu.kle.permits;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.util.*;
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasAuthority('users:manage')")
public class AdminController {
 final AdminService admin;final Users users;final Events events;
 public AdminController(AdminService a,Users u,Events e){admin=a;users=u;events=e;}
 @GetMapping("/users") public Page<Dtos.UserView> list(@RequestParam(defaultValue="0")int page){return users.findAll(PageRequest.of(Math.max(page,0),25,Sort.by("id"))).map(Dtos.UserView::of);}
 @GetMapping("/roles") public Role[] roles(){return Role.values();}
 @GetMapping("/summary") public Map<String,Long> summary(){List<AppUser> all=users.findAll();return Map.of("totalUsers",(long)all.size(),"activeAccounts",all.stream().filter(u->u.enabled&&!u.locked).count(),"lockedAccounts",all.stream().filter(u->u.locked).count());}
 @PostMapping("/users") public Dtos.UserView create(@Valid @RequestBody Dtos.CreateUser d,HttpServletRequest r){return admin.create(d,actor(r));}
 @PatchMapping("/users/{id}") public Dtos.UserView update(@PathVariable Long id,@Valid @RequestBody Dtos.UpdateUser d,HttpServletRequest r){return admin.update(id,d,actor(r));}
 @PostMapping("/users/{id}/unlock") public Dtos.UserView unlock(@PathVariable Long id,HttpServletRequest r){return admin.unlock(id,actor(r));}
 @GetMapping("/events") public Page<SecurityEvent> events(@RequestParam(defaultValue="0")int page){return events.findAllByOrderByOccurredAtDesc(PageRequest.of(Math.max(page,0),25));}
 private Long actor(HttpServletRequest r){return ((AppUser)r.getAttribute("currentUser")).id;}
}
