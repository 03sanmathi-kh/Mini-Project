package edu.kle.permits;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/reference") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
public class ReferenceController {
 private final JdbcTemplate jdbc;
 public ReferenceController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping("/analytics") @Transactional(readOnly=true) public Map<String,Object> analytics(){
  Map<String,Long> counts=new LinkedHashMap<>();for(String table:List.of("applicants","applications","documents","properties","payments","inspections","assignments","officers","corrections","registration_requests"))counts.put(table,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class));
  return Map.of("source","Live PostgreSQL database","tables",counts,"applicationStates",groups("applications","application_status"),"registrationStates",groups("registration_requests","registration_status"),"paymentStates",groups("payments","payment_status"));
 }
 private Map<String,Long> groups(String table,String column){Map<String,Long> data=new TreeMap<>();jdbc.query("SELECT "+column+",COUNT(*) AS total FROM "+table+" GROUP BY "+column,r->{data.put(r.getString(1),r.getLong(2));});return data;}
}
