package edu.kle.permits;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class MigrationUpgradeTest {
 @Test void upgradePreservesLegacyIdsDocumentsPaymentsAndUnknownPropertyValues()throws Exception{
  String url="jdbc:h2:mem:upgrade-"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
  Flyway.configure().dataSource(url,"sa","").target("12").load().migrate();JdbcTemplate j=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
  String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4).encode(UUID.randomUUID().toString());
  j.update("INSERT INTO app_users(id,full_name,email,password_hash,role,enabled,locked,failed_attempts,created_at,updated_at) VALUES(1,'Legacy applicant','legacy@permitworks.com',?,'APPLICANT',TRUE,FALSE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",hash);
  j.update("INSERT INTO app_users(id,full_name,email,password_hash,role,enabled,locked,failed_attempts,created_at,updated_at) VALUES(2,'Legacy finance','legacy-finance@permitworks.com',?,'FINANCE_OFFICER',TRUE,FALSE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",hash);
  j.update("INSERT INTO permit_applications(id,applicant_id,project_title,permit_type,property_address,construction_description,property_owner_name,contractor_architect_name,estimated_project_cost,proposed_start_date,supporting_documents,status,review_round,created_at,updated_at,version) VALUES(71,1,'Preserved project','Building','Unknown legacy address','Original proposal','Owner','Architect',100,CURRENT_DATE,'plan.pdf','APPROVED',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)");
  byte[] bytes="%PDF-1.4 legacy original".getBytes(StandardCharsets.UTF_8);String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));String content=Base64.getEncoder().encodeToString(bytes);
  j.update("INSERT INTO document_versions(id,application_id,document_name,version_no,content_type,content_hash,content_size,content_base64,uploaded_by,uploaded_at) VALUES(81,71,'plan.pdf',1,'application/pdf',?,?,?,1,CURRENT_TIMESTAMP)",sha,bytes.length,content);
  j.update("INSERT INTO payment_records(id,application_id,fee_amount,payment_status,payment_reference,recorded_by,recorded_at) VALUES(91,71,25,'PENDING','TEST-LEGACY',2,CURRENT_TIMESTAMP)");
  Flyway.configure().dataSource(url,"sa","").load().migrate();
  assertEquals("Preserved project",j.queryForObject("SELECT project_title FROM applications WHERE application_id=71",String.class));
  assertEquals(content,j.queryForObject("SELECT content_base64 FROM documents WHERE document_id=81",String.class));
  assertEquals(hash,j.queryForObject("SELECT password_hash FROM applicants WHERE applicant_id=1",String.class));
  assertEquals(1L,j.queryForObject("SELECT applicant_id FROM payments WHERE payment_id=91",Long.class));
  assertNull(j.queryForObject("SELECT street_number FROM properties WHERE property_id=(SELECT property_id FROM applications WHERE application_id=71)",String.class));
  assertEquals(19,j.queryForObject("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\"=TRUE AND \"version\" IS NOT NULL",Integer.class));
 }
}
