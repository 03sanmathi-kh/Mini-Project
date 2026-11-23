package db.migration;
import org.flywaydb.core.api.migration.*;
import java.sql.*;
import java.util.*;

/** Remove only the legacy single-column UNIQUE(application_id), preserving every payment. */
public class V14__Allow_multiple_application_payments extends BaseJavaMigration {
 public void migrate(Context context)throws Exception{
  Connection c=context.getConnection();List<String> names=new ArrayList<>();
  // H2 may reuse the child UNIQUE index for its foreign key and retain it after DROP UNIQUE.
  // Detach/recreate that FK around the index change; PostgreSQL does not need this workaround.
  List<String[]> foreignKeys=new ArrayList<>();
  if(c.getMetaData().getDatabaseProductName().equals("H2")){
   try(ResultSet r=c.getMetaData().getImportedKeys(null,"PUBLIC","PAYMENT_RECORDS")){while(r.next())if(r.getString("FKCOLUMN_NAME").equalsIgnoreCase("application_id"))foreignKeys.add(new String[]{r.getString("FK_NAME"),r.getString("PKTABLE_NAME"),r.getString("PKCOLUMN_NAME")});}
   try(Statement s=c.createStatement()){for(String[] fk:foreignKeys)s.execute("ALTER TABLE payment_records DROP CONSTRAINT \""+fk[0]+"\"");}
  }
  String q="SELECT tc.constraint_name FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage k ON tc.constraint_catalog=k.constraint_catalog AND tc.constraint_schema=k.constraint_schema AND tc.constraint_name=k.constraint_name WHERE LOWER(tc.table_schema)='public' AND LOWER(tc.table_name)='payment_records' AND tc.constraint_type='UNIQUE' GROUP BY tc.constraint_name HAVING COUNT(*)=1 AND MAX(LOWER(k.column_name))='application_id'";
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){while(r.next())names.add(r.getString(1));}
  try(Statement s=c.createStatement()){for(String name:names)s.execute("ALTER TABLE payment_records DROP CONSTRAINT \""+name.replace("\"","\"\"")+"\"");for(String[] fk:foreignKeys)s.execute("ALTER TABLE payment_records ADD CONSTRAINT \""+fk[0]+"\" FOREIGN KEY(application_id) REFERENCES \""+fk[1]+"\"(\""+fk[2]+"\")");s.execute("CREATE INDEX idx_payment_application_date ON payment_records(application_id,recorded_at,id)");}
 }
}
