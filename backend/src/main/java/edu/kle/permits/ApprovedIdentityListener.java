package edu.kle.permits;
import jakarta.persistence.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Synchronize the approved identity profiles in the same transaction as common authentication. */
@Component public class ApprovedIdentityListener {
 private final JdbcTemplate jdbc;
 public ApprovedIdentityListener(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @PostPersist @PostUpdate public void sync(AppUser u){
  int applicants=jdbc.update("UPDATE applicants SET name=?,email=?,password_hash=?,address=?,phone_no=?,updated_at=CURRENT_TIMESTAMP WHERE applicant_id=?",u.fullName,u.email,u.passwordHash,u.address,u.phoneNo,u.id);
  if(applicants==0&&u.role==Role.APPLICANT)jdbc.update("INSERT INTO applicants(applicant_id,name,email,password_hash,address,phone_no) VALUES(?,?,?,?,?,?)",u.id,u.fullName,u.email,u.passwordHash,u.address,u.phoneNo);
  int officers=jdbc.update("UPDATE officers SET name=?,email=?,password_hash=?,phone_number=?,updated_at=CURRENT_TIMESTAMP WHERE officer_id=?",u.fullName,u.email,u.passwordHash,u.phoneNo,u.id);
  if(officers==0&&u.role!=Role.APPLICANT)jdbc.update("INSERT INTO officers(officer_id,name,email,password_hash,phone_number,official_role) VALUES(?,?,?,?,?,?)",u.id,u.fullName,u.email,u.passwordHash,u.phoneNo,u.role.name());
  else if(officers>0&&u.role!=Role.APPLICANT)jdbc.update("UPDATE officers SET official_role=? WHERE officer_id=?",u.role.name(),u.id);
 }
}
