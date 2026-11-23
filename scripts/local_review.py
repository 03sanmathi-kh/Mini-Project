"""Local review operations. Credentials stay in process environment, never command arguments/logs."""
from pathlib import Path
import argparse, datetime, json, os, re, subprocess, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
PG=Path(r'C:\Program Files\PostgreSQL\17\bin')
def config():
 env=os.environ.copy()
 required=['DB_URL','DB_USER','DB_PASSWORD','JWT_SECRET','ADMIN_PASSWORD']
 if not all(env.get(k) for k in required):
  file=ROOT/'backend/.idea/workspace.xml'
  if file.exists():
   for c in ET.parse(file).getroot().iter('configuration'):
    values={e.get('name'):e.get('value') for e in c.iter('env')}
    if all(values.get(k) for k in required):
     for k,v in values.items(): env.setdefault(k,v)
     break
 if not all(env.get(k) for k in required):raise RuntimeError('Configure DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET and ADMIN_PASSWORD locally.')
 if env['DB_URL']!='jdbc:postgresql://localhost:5432/permits':raise RuntimeError('This local helper supports only the verified localhost permits database.')
 env['PGPASSWORD']=env['DB_PASSWORD'];return env
def sql(env,query,database='permits',read_only=False):
 child=env.copy()
 if read_only:child['PGOPTIONS']='-c default_transaction_read_only=on'
 r=subprocess.run([str(PG/'psql.exe'),'-X','-w','-h','localhost','-p','5432','-U',env['DB_USER'],'-d',database,'-v','ON_ERROR_STOP=1','-c',query],env=child,capture_output=True,text=True)
 if r.returncode:raise RuntimeError(r.stderr)
 return r.stdout
def main():
 parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['verify','backup','isolated-tests','start']);args=parser.parse_args();env=config();evidence=ROOT/'review_evidence/database';evidence.mkdir(exist_ok=True,parents=True)
 if args.operation=='verify':
  query="SELECT current_database(),current_user,version(); SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank; SELECT count(*) AS users FROM app_users; SELECT count(*) AS permits FROM permit_applications; SELECT table_name,column_name,is_nullable FROM information_schema.columns WHERE table_schema='public' AND table_name IN ('auth_sessions','officer_routing','document_versions','audit_log') ORDER BY table_name,ordinal_position;"
  text=sql(env,query,read_only=True);(evidence/'live-schema.txt').write_text(text,encoding='utf-8');print(text)
 elif args.operation=='backup':
  folder=ROOT/'backups';folder.mkdir(exist_ok=True);path=folder/('permits-before-sprint2-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.dump')
  r=subprocess.run([str(PG/'pg_dump.exe'),'-w','-h','localhost','-U',env['DB_USER'],'-d','permits','-Fc','-f',str(path)],env=env,capture_output=True,text=True)
  if r.returncode:raise RuntimeError(r.stderr)
  print('Backup created:',path,'bytes:',path.stat().st_size)
 elif args.operation=='isolated-tests':
  name='permitworks_review_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S');assert re.fullmatch(r'permitworks_review_[0-9_]+',name)
  sql(env,'CREATE DATABASE '+name,'postgres');env.update(TEST_DB_URL='jdbc:postgresql://localhost:5432/'+name,TEST_DB_DRIVER='org.postgresql.Driver',TEST_DB_USER=env['DB_USER'],TEST_DB_PASSWORD=env['DB_PASSWORD'])
  (evidence/'isolated-database.txt').write_text(name+'\nCreated for tests; retained without dropping any database.\n',encoding='utf-8')
  with (evidence/'postgresql-engine-tests.log').open('w',encoding='utf-8') as log:
   result=subprocess.run(['mvn.cmd','-B','-Dpermit.build.directory=target-pg-review','-Dtest=EngineIntegrationTest','test'],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT)
  print('PostgreSQL engine tests exit:',result.returncode,'database:',name)
  (evidence/'isolated-integrity.txt').write_text(sql(env,"SELECT version,success FROM flyway_schema_history ORDER BY installed_rank; SELECT count(*) AS orphan_assignments FROM application_assignments a LEFT JOIN permit_applications p ON p.id=a.application_id WHERE p.id IS NULL; SELECT count(*) AS audit_rows FROM audit_log;",name,True),encoding='utf-8')
  raise SystemExit(result.returncode)
 elif args.operation=='start':
  env.setdefault('ALLOWED_ORIGIN','http://localhost:5174')
  jar=ROOT/'backend/target-review/permit-engine1-1.0.0.jar'
  if not jar.exists():raise RuntimeError('Package the backend first.')
  log=(ROOT/'review_evidence/integration/backend-startup.log').open('w',encoding='utf-8')
  process=subprocess.Popen(['java.exe','-jar',str(jar)],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
  (ROOT/'review_evidence/integration/backend-process.json').write_text(json.dumps({'pid':process.pid,'port':8080}),encoding='utf-8');print('Started backend process',process.pid)
if __name__=='__main__': main()
