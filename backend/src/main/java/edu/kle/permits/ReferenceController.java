package edu.kle.permits;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/reference") @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_OFFICER')")
public class ReferenceController {
 private final Path summary;private final ObjectMapper json;
 public ReferenceController(@Value("${app.reference-summary:../reference_data/summary.json}") String path,ObjectMapper json){summary=Path.of(path);this.json=json;}
 @GetMapping("/analytics") public JsonNode analytics()throws java.io.IOException{
  if(!Files.isRegularFile(summary))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Reference data has not been staged. Run scripts/stage_reference.py.");
  return json.readTree(Files.readString(summary));
 }
}
