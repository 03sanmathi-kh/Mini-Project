package edu.kle.permits;

import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
import java.security.*;
import java.util.HexFormat;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/permits/{applicationId}/documents/files")
public class DocumentController {
 private final PermitService service;private final PermitRecords permits;private final DocumentVersions versions;private final AuditService audit;private final Clock clock;
 public DocumentController(PermitService s,PermitRecords p,DocumentVersions v,AuditService a,Clock c){service=s;permits=p;versions=v;audit=a;clock=c;}
 public record Metadata(Long id,String name,int version,String contentType,long size,String sha256,Instant uploadedAt){}
 private Metadata metadata(DocumentVersion d){return new Metadata(d.id,d.documentName,d.versionNo,d.contentType,d.contentSize,d.contentHash,d.uploadedAt);}
 @GetMapping public List<Metadata> list(@PathVariable Long applicationId,HttpServletRequest r){service.get(applicationId,actor(r));return versions.findByApplicationIdOrderByUploadedAt(applicationId).stream().map(this::metadata).toList();}
 @PostMapping @Transactional public Metadata upload(@PathVariable Long applicationId,@RequestParam("file") MultipartFile file,HttpServletRequest r)throws Exception{
  AppUser actor=actor(r);PermitRecord p=permits.lockById(applicationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found."));
  if(actor.role!=Role.APPLICANT||!p.applicantId.equals(actor.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the owner may upload documents.");
  if(p.status!=PermitStatus.DRAFT&&p.status!=PermitStatus.CORRECTION_REQUIRED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Documents can change only in draft or correction stage.");
  String name=file.getOriginalFilename();if(name==null||!name.matches("[A-Za-z0-9][A-Za-z0-9 ._-]{0,149}")||name.contains("..")||file.isEmpty()||file.getSize()>5242880)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a safe filename and a nonempty PDF, PNG or JPEG up to 5 MB.");
  byte[] content=file.getBytes();String type=detect(content);if(type==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PDF, PNG and JPEG content is accepted.");
  DocumentVersion d=new DocumentVersion();d.applicationId=applicationId;d.documentName=name;d.versionNo=versions.findFirstByApplicationIdAndDocumentNameOrderByVersionNoDesc(applicationId,name).map(v->v.versionNo+1).orElse(1);
  d.contentType=type;d.contentSize=content.length;d.contentBase64=Base64.getEncoder().encodeToString(content);d.contentHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));d.uploadedBy=actor.id;d.uploadedAt=clock.instant();versions.save(d);
  audit.record(actor,"DOCUMENT_UPLOADED","permit",applicationId,"name="+name+"; version="+d.versionNo+"; sha256="+d.contentHash);return metadata(d);
 }
 @GetMapping("/{id}") public ResponseEntity<byte[]> download(@PathVariable Long applicationId,@PathVariable Long id,HttpServletRequest r){
  service.get(applicationId,actor(r));DocumentVersion d=versions.findById(id).filter(v->v.applicationId.equals(applicationId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found."));
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType)).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.documentName).build().toString()).body(Base64.getDecoder().decode(d.contentBase64));
 }
 private String detect(byte[] b){if(b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-')return "application/pdf";if(b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10)return "image/png";if(b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";return null;}
 private AppUser actor(HttpServletRequest r){return (AppUser)r.getAttribute("currentUser");}
}
