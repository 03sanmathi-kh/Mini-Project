from pathlib import Path
import hashlib,json,re,subprocess
from local_review import config
ROOT=Path(__file__).resolve().parents[1]
def edit(name,old,new):
 p=ROOT/name;s=p.read_text(encoding='utf-8');p.write_text(s.replace(old,new),encoding='utf-8')
edit('README.md','Sessions remain valid during inactivity until that limit, explicit logout, or administrative revocation.','Sessions expire after 30 minutes idle, eight hours absolutely, explicit logout, or administrative revocation. Read-only polling never refreshes activity.')
edit('README.md','Flyway V1 through V4','Flyway V1 through V8')
edit('README.md','Supporting documents are references/names rather than uploaded binary files.','Supporting documents can now be uploaded as immutable PDF/PNG/JPEG byte versions; old reference strings are retained separately.')
edit('README.md','outbound email/SMS, production TLS, malware scanning, and database-role-level prevention of direct history-table modification','real outbound email/SMS, production TLS and malware scanning')
edit('README.md','session expiry/revocation; and logout.','idle/absolute session expiry, revocation and logout. New engine cases additionally exercise concurrent writes, failure injection, documents, notifications, routing and report filters.')
edit('INTEGRATION.md','Admin creation uses `{fullName,localPart,password,role}`.','Admin creation uses `{fullName,localPart,personalContactEmail,password,role}`.')
edit('INTEGRATION.md','401 for an invalid session,','423 for locked login, 401 for invalid credentials/session,')
edit('INTEGRATION.md','Reassignment deactivates the prior assignment but preserves its row.','Reassignment requires explicit confirmation and a reason, deactivates the prior assignment and preserves previous/new officer IDs. E4 owns selection/persistence; E2 advances workflow state. Completed review ownership remains visible but is excluded from unfinished workload.')
for name in ['REVIEW_DEMO_GUIDE.md','FINAL_IMPLEMENTATION_REPORT.md','DATABASE_VERIFICATION.md','VALIDATION.md']:
 p=ROOT/name;s=p.read_text(encoding='utf-8').split('\n\nFinal live check:')[0]
 s+='\n\nFinal live check: PostgreSQL V1-V9 applied successfully, eight original users and two original permits preserved. Live API login/read/activity/logout checks passed, including reuse rejection of the revoked cookie. Browser smoke passed login, workload, reference analytics, audit and notifications with zero page errors. Screenshots hide contact identity and blur officer names. Evidence: review_evidence/database/live-schema.txt; review_evidence/integration/live-api-smoke.json and browser-smoke.json.\n'
 if name=='REVIEW_DEMO_GUIDE.md':s+='\nCurrent running UI: http://localhost:5174 (5173 belongs to another project). Use `$env:ALLOWED_ORIGIN=\'http://localhost:5174\'` before backend start and `--port 5174` for Vite. The normal default remains 5173 when available.\n'
 p.write_text(s,encoding='utf-8')
p=ROOT/'frontend/src/Permit.jsx';p.write_text('\n'.join(line.rstrip() for line in p.read_text(encoding='utf-8').splitlines())+'\n',encoding='utf-8')
data=json.loads((ROOT/'review_evidence/documents/test-results.json').read_text(encoding='utf-8'))
viva={
 'E1_AKHIL_TECHNICAL_GUIDE.md':[
  ('How is a password checked?','BCryptPasswordEncoder uses cost 12. The app compares a candidate with the stored one-way hash; it never decrypts a password. Creation requires 8-72 UTF-8 bytes, while a bounded nonblank wrong login candidate returns 401 and counts as a failed attempt.'),
  ('What do JWT and server sessions each provide?','The signed JWT carries issuer, user subject, session ID, email and expiry. The server session supplies revocation and idle tracking. The filter reloads the enabled/unlocked user and current role from the database rather than trusting a browser role.'),
  ('Why HttpOnly and CSRF together?','HttpOnly prevents JavaScript from reading PERMIT_TOKEN. Browsers still send that cookie automatically, so mutations require the XSRF cookie/header pair. The XSRF cookie is readable so the frontend can send the current token after rotation; it is not the authentication token.'),
  ('What extends a session?','Successful mutations or POST /api/auth/activity before expiry update lastActivity. Real pointer/key input is throttled to one activity request per minute. GET polling never extends idle time. Exactly 30 idle minutes expires; no activity extends the eight-hour absolute limit.'),
  ('How do you demonstrate failures?','Show missing/tampered token denial, wrong-role 403, short wrong password 401, fifth failure/locked login 423, Admin-only unlock, disabled-user revocation and logout-token reuse rejection. Controlled Clock tests demonstrate time boundaries without waiting real hours.')],
 'E2_TECHNICAL_GUIDE.md':[
  ('Who controls permit status?','PermitService controls status and logs each mutation transactionally. E4 selects and persists assignees; E2 moves the permit to the next review stage after allocation. Applicants cannot submit arbitrary status or actor IDs.'),
  ('How are premature decisions blocked?','decide accepts only PENDING_FINAL_DECISION, reached through guarded technical and zoning review actions. Wrong roles fail authorization; invalid stage changes return 409 without writing false history.'),
  ('What makes resubmission valid?','An open correction is required and content must differ from the previous submission snapshot, or a later document file version must exist. A response string alone does not satisfy the rule. The review round increments while previous reviews and snapshots remain.'),
  ('What happens when inspection cannot finish?','COULD_NOT_COMPLETE requires observations. The result is retained and the permit returns to an issued/reschedulable state. It cannot close until a successful final inspection exists and the current stage is inspection passed.'),
  ('What is real in notifications and payment?','In-app notifications are persisted and filtered to the recipient; read state is enforced by owner ID. SMS is a local provider mock with failure fallback, never a delivery claim. Payment records are explicitly simulated, not real bank transactions.')],
 'E3_TECHNICAL_GUIDE.md':[
  ('What proves atomicity?','A spy injects an exception after permit/history/audit writes inside a service transaction. The test checks unchanged counts afterward. Separate FK tests attempt invalid application, applicant and officer references. No operational database record is a failure fixture.'),
  ('Why locks and versions?','Permit mutation acquires PESSIMISTIC_WRITE inside a transaction. The entity also has a Hibernate version column. Allocation additionally locks a database mutex before the permit, serializing least-load choices across concurrent requests and application instances.'),
  ('How are audit records protected?','The API exposes read-only audit/history operations with role and visibility checks. PostgreSQL V8 rejects UPDATE, DELETE and TRUNCATE on audit_log, workflow_history and document_versions. A superuser can disable triggers, so this is not a malicious-superuser guarantee.'),
  ('How do real document versions differ from text references?','Each uploaded version stores immutable bytes as base64 plus application/name/version, content type, size, SHA-256, uploader and time. Owner/status checks protect uploads and permit visibility protects downloads. CSV FilePath strings do not create actual files.'),
  ('What does the recovery result establish?','The measured ROLLBACK_RECOVERY_MS verifies restoration after a deliberate transaction failure. It does not certify process crash, power-loss durability or a ten-second recovery target. V9 separately derives old assignment completion from current-round review evidence and does not rewrite history.')],
 'E4_TECHNICAL_GUIDE.md':[
  ('How is eligibility established?','A saved profile matches role, permit type, zone and assignment stage; the account must be enabled and unlocked. The permit state must allow that stage. A wildcard is an explicit configuration choice, not an assumption that every officer covers every zone.'),
  ('How does the heap choose?','Candidates are ordered by unfinished active assignment count, then lowest officer ID. Repeated insertion builds the current heap in O(n log n); removal is O(log n). The database queries used to count workloads also contribute to total cost; this is not a load benchmark.'),
  ('What if no officer is eligible?','No invalid assignment is made. A unique permit/stage queue entry records the reason and a Department Officer in-app alert is persisted. Configure a matching profile and retry; do not invent an officer or silently advance the workflow.'),
  ('What permits a replacement?','Only Admin or Department Officer can manage routing. Replacing an existing assignment requires explicit confirmation and a nonblank reason; the old row is deactivated, previous/new IDs and timestamp are retained and an audit record is appended.'),
  ('How are deadlines and CSV analytics interpreted?','A configured SLA sets a deadline for new allocations. Missing SLA is reported as unconfigured, not overdue. V9 fixes historical workload flags using current-round reviews. CSV analytics use an isolated reference store with ambiguous roles quarantined and no credential or document-file import.')]
}
for name,items in viva.items():
 p=ROOT/name;text=p.read_text(encoding='utf-8').split('\n\n## Viva questions and answers')[0]+'\n\n## Viva questions and answers\n\n'
 text+='\n\n'.join('**'+q+'**\n\n'+a for q,a in items)+'\n';p.write_text(text,encoding='utf-8')
for engine in ['E1','E2','E3','E4']:
 selected=[dict(case,database=db) for db in ['h2','postgresql'] for case in data[db] if case.get('engine')==engine]
 # Earlier JSON emission precedes enrichment; use stable class/method classification if needed.
 if not selected:
  for db in ['h2','postgresql']:
   for case in data[db]:
    method=case['method'];klass=case['class']
    owner='E4' if method.startswith('TC_E4') else 'E3' if method.startswith('TC_E3') or klass=='ResetScriptTest' else 'E2' if method.startswith('TC_E2') or klass=='PermitWorkflowIntegrationTest' else 'E1'
    if owner==engine:selected.append(dict(case,database=db))
 folder=ROOT/'review_evidence'/engine
 (folder/'test-results.json').write_text(json.dumps(selected,indent=2),encoding='utf-8')
 (folder/'README.md').write_text(f'# {engine} evidence\n\nActual test cases and execution times are in test-results.json. Commands, expected behavior and evidence paths are in FINAL_TEST_RESULTS.md. PostgreSQL cases use a separate retained test database; the operational database was not used for failure injection.\n',encoding='utf-8')
checklist='''# Final review checklist

- [x] Existing source/history preserved; initial patch and Git status retained privately.
- [x] E4 eligible heap selection, queue, confirmed override, deadlines and report filters implemented/tested.
- [x] E1 idle/absolute limits, clock, lockout, CSRF cookie refresh and role boundaries verified.
- [x] E2 workflow, actual-change resubmission, notifications and incomplete inspections verified.
- [x] E3 rollback, concurrent writes, FK, documents and PostgreSQL audit trigger verified.
- [x] PostgreSQL live V1-V9 verified; original eight users and two permits preserved.
- [x] Full backend verify passed: 46 cases, 45 passed and one PostgreSQL-only skip.
- [x] Separate PostgreSQL engine run: 20/20 passed.
- [x] Frontend build and three staging tests passed; browser/API smoke passed.
- [x] Original ZIP preserved; isolated reference import/quarantine and analytics produced.
- [x] Handbook copy and 21-slide PPT generated, rendered and checked.
- [x] Genuine defect/retest and FR/NFR traceability reports provided.
- [ ] Guide approval of handbook/PPT, routing/SLA choices and design evolution.
- [ ] Each student's individual rehearsal and AI Mentor activity.
- [ ] Designated Drive upload and any authorized GitHub push.
- [ ] Real SMS delivery, 50-user load target and human usability study.
- [ ] Forced-crash recovery and ten-second recovery target certification.

Unchecked items are intentionally not claimed complete. Local work remains uncommitted; no remote push occurred.
'''
(ROOT/'FINAL_REVIEW_CHECKLIST.md').write_text(checklist,encoding='utf-8')
# Sanitize real credentials plus transient JWT/CSRF/test-password values in text evidence.
private=config();secret_values=[private[k] for k in ['DB_PASSWORD','JWT_SECRET','ADMIN_PASSWORD'] if private.get(k)]
redactions=0
for p in (ROOT/'review_evidence').rglob('*'):
 if p.suffix not in {'.txt','.log','.json','.md'} or not p.is_file():continue
 text=p.read_text(encoding='utf-8',errors='replace');before=text
 for secret in secret_values:
  if len(secret)>=8:text=text.replace(secret,'[REDACTED]')
  else:text=re.sub(r'(?<![A-Za-z0-9])'+re.escape(secret)+r'(?![A-Za-z0-9])','[REDACTED]',text)
 text=re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[JWT_REDACTED]',text)
 text=re.sub(r'("password"\s*:\s*")[^"]*(")',r'\1[REDACTED]\2',text,flags=re.I)
 text=re.sub(r'(?i)((?:X-)?XSRF-TOKEN[=:]\s*\[?)[A-Za-z0-9_-]{20,}',r'\1[CSRF_REDACTED]',text)
 if text!=before:p.write_text(text,encoding='utf-8');redactions+=1
git=lambda *args:subprocess.run(['git',*args],cwd=ROOT,capture_output=True,text=True).stdout
(ROOT/'review_evidence/git/final-status.txt').write_text(git('status','--short'),encoding='utf-8')
(ROOT/'review_evidence/git/final-diff-stat.txt').write_text(git('diff','--stat'),encoding='utf-8')
(ROOT/'GIT_EVIDENCE.md').write_text('# PermitWorks Git evidence\n\nBranch '+git('branch','--show-current').strip()+'; initial and current HEAD '+git('rev-parse','HEAD').strip()+'. Original history preserved. New work is not committed; no authorship or remote push is invented. Pre-existing patch is ignored/private.\n\nRecent all-ref history:\n\n```text\n'+git('log','--all','-4','--format=%h %ad %s','--date=short')+'```\n\nFinal status and tracked diff summaries: review_evidence/git/final-status.txt and final-diff-stat.txt.\n',encoding='utf-8')
manifest={}
for name in ['backend/target-review/permit-engine1-1.0.0.jar','review_evidence/documents/SPRINT2_STUDENT_HANDBOOK_UPDATED.docx','review_evidence/documents/SPRINT2_REVIEW_PRESENTATION_v9.pptx','reference_data/summary.json']:
 p=ROOT/name;manifest[name]={'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
(ROOT/'review_evidence/integration/deliverable-checksums.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
print('Final reports/evidence updated. Text evidence files sanitized:',redactions)
