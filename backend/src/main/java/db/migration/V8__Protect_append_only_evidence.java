package db.migration;

import org.flywaydb.core.api.migration.*;

/** PostgreSQL protection complements the API's absence of history update/delete routes. */
public class V8__Protect_append_only_evidence extends BaseJavaMigration {
 public void migrate(Context context)throws Exception{
  if(!context.getConnection().getMetaData().getDatabaseProductName().equals("PostgreSQL"))return;
  try(var s=context.getConnection().createStatement()){
   s.execute("CREATE FUNCTION permitworks_reject_history_change() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'PermitWorks evidence is append-only'; END $$");
   for(String table:new String[]{"audit_log","workflow_history","document_versions"}){
    s.execute("CREATE TRIGGER immutable_evidence BEFORE UPDATE OR DELETE ON "+table+" FOR EACH ROW EXECUTE FUNCTION permitworks_reject_history_change()");
    s.execute("CREATE TRIGGER immutable_evidence_truncate BEFORE TRUNCATE ON "+table+" FOR EACH STATEMENT EXECUTE FUNCTION permitworks_reject_history_change()");
   }
  }
 }
}
