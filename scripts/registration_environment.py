"""Local registration verification/deployment. Secrets are never put in command arguments or evidence."""
from pathlib import Path
import argparse,datetime,json,os,signal,subprocess,secrets,time,re
from local_review import config,sql,ROOT
OUT=ROOT/'FINAL_GATE3_DELIVERABLES';EVIDENCE=OUT/'evidence';PRIVATE=OUT/'private-build'
NODE=r'C:\Program Files\WindowsApps\OpenAI.CodexPrimaryRuntime.v26-1119-907-0_26.1119.907.0_x64__3k8sg7r9htsxt\dependencies\node\bin\node.exe'
def stamp():return datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
def create_database(env,prefix):
 name=prefix+stamp();assert re.fullmatch('[a-z0-9_]+',name);sql(env,'CREATE DATABASE '+name,'postgres');return name
def start_backend(env,logname):
 jar=ROOT/'backend/target-registration/permit-engine1-1.0.0.jar'
 if not jar.exists():raise RuntimeError('Run Maven verify/package first.')
 log=(EVIDENCE/logname).open('w',encoding='utf-8')
 return subprocess.Popen(['java.exe','-jar',str(jar)],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
def snapshot(env):
 result={}
 for table in ['app_users','permit_applications','workflow_history','audit_log','support_requests','application_assignments','application_reviews']:
  maximum=int(sql(env,f'SELECT COALESCE(MAX(id),0) FROM {table}',read_only=True).splitlines()[2].strip())
  # Limit later comparisons to original IDs. Exclude mutable login counters/timestamps for users.
  fields='id,full_name,email,personal_contact_email,password_hash,role,enabled,locked,created_at' if table=='app_users' else '*'
  digest=sql(env,f"SELECT count(*),md5(COALESCE(string_agg(row_to_json(t)::text,'|' ORDER BY id),'')) FROM (SELECT {fields} FROM {table} WHERE id<={maximum}) t",read_only=True)
  result[table]={'maximum':maximum,'fields':fields,'signature':digest}
 return result
def main():
 parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['baseline','pg-tests','demo','deploy','preservation','stop-demo']);args=parser.parse_args();env=config();EVIDENCE.mkdir(exist_ok=True,parents=True);PRIVATE.mkdir(exist_ok=True,parents=True)
 if args.operation=='baseline':
  (EVIDENCE/'original-record-signatures.json').write_text(json.dumps(snapshot(env),indent=2),encoding='utf-8');print('Read-only signatures recorded for original users, permits, history, audit, support, assignments and reviews.')
 elif args.operation=='pg-tests':
  name=create_database(env,'permitworks_registration_test_');env.update(TEST_DB_URL='jdbc:postgresql://localhost:5432/'+name,TEST_DB_DRIVER='org.postgresql.Driver',TEST_DB_USER=env['DB_USER'],TEST_DB_PASSWORD=env['DB_PASSWORD'])
  (EVIDENCE/'registration-test-database.txt').write_text(name+'\nRetained isolated database; no database dropped.\n',encoding='utf-8')
  with (EVIDENCE/'registration-postgresql-tests.log').open('w',encoding='utf-8') as log:r=subprocess.run(['mvn.cmd','-B','-Dpermit.build.directory=target-registration-pg','-Dtest=RegistrationIntegrationTest','test'],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT)
  print('PostgreSQL registration test exit:',r.returncode);raise SystemExit(r.returncode)
 elif args.operation=='demo':
  name=create_database(env,'permitworks_registration_demo_');password=secrets.token_urlsafe(24)
  env.update(DB_URL='jdbc:postgresql://localhost:5432/'+name,JWT_SECRET=secrets.token_urlsafe(48),ADMIN_PASSWORD=password,ADMIN_LOCAL_PART='admin',SERVER_PORT='8081',ALLOWED_ORIGIN='http://localhost:5175')
  backend=start_backend(env,'demo-backend.log');frontend_env=os.environ.copy();frontend_env['API_PROXY_TARGET']='http://localhost:8081'
  frontend=subprocess.Popen([NODE,'node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','5175','--configLoader','runner'],cwd=ROOT/'frontend',env=frontend_env,stdout=(EVIDENCE/'demo-frontend.log').open('w'),stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
  (PRIVATE/'demo-access.json').write_text(json.dumps({'adminEmail':'admin@permitworks.com','adminPassword':password,'uiURL':'http://localhost:5175','database':name}),encoding='utf-8')
  (EVIDENCE/'demo-processes.json').write_text(json.dumps({'backend':backend.pid,'frontend':frontend.pid,'database':name,'uiPort':5175,'apiPort':8081}),encoding='utf-8');print('Isolated demo launched; credentials stored only in ignored private-build.')
 elif args.operation=='deploy':
  pid_file=ROOT/'review_evidence/integration/backend-process.json'
  if pid_file.exists():
   pid=json.loads(pid_file.read_text())['pid']
   # Verify the exact process still belongs to this application before stopping it.
   command=subprocess.run(['powershell','-NoProfile','-Command',f'(Get-CimInstance Win32_Process -Filter "ProcessId={int(pid)}").CommandLine'],capture_output=True,text=True).stdout
   if 'permit-engine1-1.0.0.jar' not in command:raise RuntimeError('Recorded PID no longer identifies PermitWorks; refusing to stop another process.')
   os.kill(pid,signal.SIGTERM)
  env['SERVER_PORT']='8080';env['ALLOWED_ORIGIN']='http://localhost:5174';process=start_backend(env,'live-registration-backend.log')
  pid_file.write_text(json.dumps({'pid':process.pid,'port':8080}),encoding='utf-8');print('Updated live backend started; existing database and JWT key retained.')
 elif args.operation=='preservation':
  before=json.loads((EVIDENCE/'original-record-signatures.json').read_text());results={}
  for table,record in before.items():
   text=sql(env,f"SELECT count(*),md5(COALESCE(string_agg(row_to_json(t)::text,'|' ORDER BY id),'')) FROM (SELECT {record['fields']} FROM {table} WHERE id<={record['maximum']}) t",read_only=True)
   results[table]='PASS' if text==record['signature'] else 'FAIL'
  meta=sql(env,"SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank; SELECT count(*) AS original_users FROM app_users; SELECT count(*) AS permits FROM permit_applications; SELECT count(*) AS pending_legacy_accounts FROM app_users WHERE activation_pending=TRUE; SELECT count(*) AS registrations FROM registration_requests;",read_only=True)
  (EVIDENCE/'live-v10-schema.txt').write_text(meta,encoding='utf-8');(EVIDENCE/'record-preservation.json').write_text(json.dumps(results,indent=2),encoding='utf-8');print(json.dumps(results));assert all(v=='PASS' for v in results.values())
 elif args.operation=='stop-demo':
  p=EVIDENCE/'demo-processes.json'
  if p.exists():
   for key in ['backend','frontend']:
    try:os.kill(json.loads(p.read_text())[key],signal.SIGTERM)
    except ProcessLookupError:pass
   print('Task-owned demo services stopped; isolated database retained.')
if __name__=='__main__':main()
