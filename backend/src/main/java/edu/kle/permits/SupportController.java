package edu.kle.permits;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/support")
public class SupportController {
 final SupportService support;public SupportController(SupportService support){this.support=support;}
 @PostMapping public SupportDtos.Created create(@Valid @RequestBody SupportDtos.Create input,HttpServletRequest request){return support.create(input,request.getRemoteAddr());}
 @PostMapping("/status") public SupportDtos.PublicStatus status(@Valid @RequestBody SupportDtos.StatusCheck input){return support.status(input);}
}
