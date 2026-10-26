package edu.kle.permits;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResetScriptTest {
 @Test void developmentResetIsGuardedOrderedAndPreservesFlywayAndAdmin() throws Exception {
  String sql=Files.readString(Path.of("..","scripts","reset-local-demo-data.sql")).toLowerCase();
  assertTrue(sql.contains("begin;"));
  assertTrue(sql.contains("current_database() <> 'permits'"));
  assertTrue(sql.contains("inet_server_addr()"));
  assertTrue(sql.contains("admin_count <> 1"));
  assertTrue(sql.contains("delete from app_users where role <> 'admin'"));
  assertTrue(sql.indexOf("delete from inspections")<sql.indexOf("delete from permit_applications"));
  assertTrue(sql.indexOf("delete from auth_sessions")<sql.indexOf("delete from app_users"));
  assertFalse(sql.matches("(?s).*(delete|update|truncate)\\s+(table\\s+)?flyway_schema_history.*"));
  assertFalse(sql.contains("password_hash"));
  assertTrue(sql.contains("raise exception"));
  assertTrue(sql.contains("commit;"));
 }
}
