package edu.kle.permits;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/support") @PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {
 final SupportService support;public AdminSupportController(SupportService support){this.support=support;}
 @GetMapping public Page<SupportDtos.Summary> list(@RequestParam(required=false)String query,@RequestParam(required=false)SupportStatus status,@RequestParam(required=false)SupportCategory category,@RequestParam(defaultValue="0")int page){return support.list(query,status,category,page);}
 @GetMapping("/count") public java.util.Map<String,Long> count(){return java.util.Map.of("open",support.openCount());}
 @GetMapping("/{id}") public SupportDtos.AdminView get(@PathVariable Long id){return support.get(id);}
 @PatchMapping("/{id}") public SupportDtos.AdminView update(@PathVariable Long id,@Valid @RequestBody SupportDtos.Update input,HttpServletRequest request){return support.update(id,input,actor(request));}
 @PostMapping("/{id}/unlock") public SupportDtos.AdminView unlock(@PathVariable Long id,HttpServletRequest request){return support.unlock(id,actor(request));}
 @PostMapping("/{id}/enable") public SupportDtos.AdminView enable(@PathVariable Long id,HttpServletRequest request){return support.enable(id,actor(request));}
 private Long actor(HttpServletRequest r){return ((AppUser)r.getAttribute("currentUser")).id;}
}
