"""Build review records from actual Surefire output and preserve the supplied DOCX package."""
from pathlib import Path
import copy, hashlib, json, re, subprocess, zipfile, xml.etree.ElementTree as E
from lxml import etree as L
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'review_evidence/documents';OUT.mkdir(exist_ok=True,parents=True)
def write(name,text): (ROOT/name).write_text(text.strip()+'\n',encoding='utf-8')
def read(p):return p.read_text(encoding='utf-8',errors='replace')
def tests(directory):
 records=[]
 for p in sorted(directory.glob('TEST-*.xml')):
  root=E.parse(p).getroot()
  for c in root.findall('testcase'):
   status='FAIL' if c.find('failure') is not None or c.find('error') is not None else 'SKIP' if c.find('skipped') is not None else 'PASS'
   records.append({'class':c.get('classname').split('.')[-1],'method':c.get('name'),'status':status,'seconds':float(c.get('time','0')),'evidence':str(p.relative_to(ROOT)).replace('\\','/')})
 return records
cases=tests(ROOT/'backend/target-review/surefire-reports');pg=tests(ROOT/'backend/target-pg-review/surefire-reports')
totals=lambda rs:{k:sum(r['status']==k for r in rs) for k in ['PASS','FAIL','SKIP']}
h=totals(cases);g=totals(pg)
summary=f"H2 regression: {len(cases)} executions, {h['PASS']} passed, {h['FAIL']} failed, {h['SKIP']} PostgreSQL-only skipped. PostgreSQL engine run: {len(pg)} executions, {g['PASS']} passed, {g['FAIL']} failed, {g['SKIP']} skipped."
csv=json.loads(read(ROOT/'reference_data/summary.json'))
(OUT/'test-results.json').write_text(json.dumps({'h2':cases,'postgresql':pg,'summary':summary},indent=2),encoding='utf-8')
def scenario(method):return re.sub(r'(?<=[a-z])(?=[A-Z])',' ',method.replace('_',' '))
details={
 'TC_E4_01':('Two eligible officers, then two ready permits','Lowest ID on tie; second permit selects lower workload; repeated call stays idempotent'),
 'TC_E4_02':('Ready permit without matching routing profile','No assignment/state corruption; queue entry and Department Officer notification'),
 'TC_E4_03':('Existing assignee; unauthorized/unconfirmed and confirmed replacement','403 or 409 for invalid override; previous officer and reason retained on success'),
 'TC_E4_04':('Configured SLA then expired due_at; another assignment without SLA','Expired assignment flagged; missing deadline reported separately'),
 'TC_E4_05':('Applicant and Department Officer call workload API','Applicant 403; Department Officer 200 with officer data'),
 'TC_E4_06':('Two synchronized allocation threads for different and identical permits','Balanced eligible choices; same-permit requests leave one active assignment'),
 'TC_E4_07':('Wrong role, mismatched zone, locked officer','Invalid profile rejected; ineligible officers never selected'),
 'TC_E4_08':('Date/type filters and reversed date range','Filtered counts equal one fixture; absent type gives zero; reversed dates rejected'),
 'TC_E3_01':('Injected exception after draft/history/audit writes','All writes roll back, counts unchanged'),
 'TC_E3_02':('Invalid applicant/officer/application/payment/review/inspection references','Foreign keys reject every attempted invalid write'),
 'TC_E3_03':('Two simultaneous technical approvals','One success and one conflict, exactly one review'),
 'TC_E3_04':('Two simultaneous paid-payment requests','One success and one conflict, exactly one payment'),
 'TC_E3_05':('Two PDF fixture versions, cross-owner download and unsafe upload','Old bytes retained; version increments; access and filename/content restrictions hold'),
 'TC_E3_06':('Two simultaneous submissions','One success, one conflict, one submission snapshot'),
 'TC_E3_07':('Deliberately failed transaction','Original title restored; rollback latency measured; no crash recovery claim'),
 'TC_E3_08':('Direct UPDATE of audit row on isolated PostgreSQL','Append-only database trigger rejects mutation; skipped on H2'),
 'TC_E3_09':('Legacy completion flags reset; current-round reviews then a new review round','Backfill removes completed reviews from workload without changing history; new-round work remains pending'),
 'TC_E2_01':('Unchanged correction response then real content edit','Unchanged resubmission 409; edited submission increments round'),
 'TC_E2_02':('Issued permit; Could Not Complete inspection with reason','Cannot close; rescheduling succeeds'),
 'TC_E2_03':('SMS mock failure; other recipient then owner marks notification read','In-app notice persists with FAILED SMS state; other recipient 404; owner succeeds'),
}
def detail(r):
 for prefix,d in details.items():
  if r['method'].startswith(prefix):return d
 return ('Isolated fixture and requests defined in '+r['class'],scenario(r['method'])+'; all assertions must hold')
testrows=[]
for r in cases+pg:
 inp,expected=detail(r);engine='E4' if 'TC_E4' in r['method'] else 'E3' if 'TC_E3' in r['method'] else 'E2' if 'TC_E2' in r['method'] or r['class']=='PermitWorkflowIntegrationTest' else 'E1' if r['class'] in ['SecurityIntegrationTest','SupportIntegrationTest','AccessPolicyTest'] else 'E3'
 r.update(engine=engine,input=inp,expected=expected,database='PostgreSQL' if r in pg else 'H2')
 testrows.append(f"| {r['method']} | {engine} / {r['database']} | {inp} | {expected} | {r['status']} | {r['seconds']:.3f} | {r['evidence']} |")
write('FINAL_TEST_RESULTS.md',f'''# PermitWorks test results

Verified 23 November 2026. {summary}

The complete final source is checked with `mvn -B -Dpermit.build.directory=target-review verify`. PostgreSQL tests run in a separately created local database through `python scripts/local_review.py isolated-tests`; no operational records are test fixtures. Both suites apply Flyway and Hibernate schema validation. Database-specific trigger enforcement is verified on PostgreSQL, not inferred from H2.

Frontend: native bundled Node ran `node_modules/vite/bin/vite.js build --configLoader runner`; 91 modules, production bundle generated. No pre-existing frontend test command exists. CSV staging: three Python unittest cases passed. Browser verification is recorded separately in integration evidence.

Test IDs use source method names, which remain stable and directly searchable. Inputs are isolated fixtures; detailed payloads/assertions are in the corresponding test class. PASS means the recorded assertions passed, not that every broader NFR is satisfied.

| Test ID | Engine/database | Input or precondition | Expected and checked behavior | Actual | Seconds | Evidence |
|---|---|---|---|---|---:|---|
{chr(10).join(testrows)}

Limitations: no human usability study, 50-user network/load certification, real SMS delivery, forced process crash or 10-second crash-recovery certification. Rollback latency appears as ROLLBACK_RECOVERY_MS in the actual logs. Tests using MockMvc role fixtures verify endpoint boundaries; the existing E1 security suite separately exercises JWT cookies, CSRF and login.
''')
rtm=[
 ('FR-01','Login and account access','E1','AuthService / SecurityConfig / AdminService','/api/auth/*; /api/admin/users','SecurityIntegrationTest','PASS','BCrypt, sessions, CSRF, roles and lockout tested'),
 ('FR-02','Create and submit applications','E2','PermitService.create/edit/submit','/api/permits','PermitWorkflowIntegrationTest; TC_E3_06','PASS','Existing flow retained; creation/API evolution needs guide review'),
 ('FR-03','Eligible workload-based assignment and override','E4','AssignmentEngine','/api/permits/{id}/auto-assignment/{type}; /override/{type}','TC_E4_01 through TC_E4_08','PASS','Explicit profiles and optional SLA; actual routing/SLA choices need approval'),
 ('FR-04','Correction, resubmission and document versions','E2/E3','PermitService.resubmit; DocumentController','/api/permits/{id}/resubmit; /documents/files','TC_E2_01; TC_E3_05; existing full flow','PASS','File-type allowlist and 5 MB limit; malware scanning not implemented'),
 ('FR-05','Valid status transitions','E2','PermitService','/api/permits/{id} action routes','Existing invalid transition/full-flow tests','PASS','Granular states differ from original diagram; approval pending'),
 ('FR-06','Review-dependent senior decision','E2','PermitService.decide','/api/permits/{id}/decision','Existing full-flow/rejection tests','PASS','Implemented HTTP contract differs from original 422 design'),
 ('FR-07','Fee and simulated payment','E2/E3','PermitService.payment','/api/permits/{id}/payment','Full flow; TC_E3_04','PASS','Simulation only; payment changes workflow status as documented'),
 ('FR-08','Inspection and closure','E2','PermitService.recordInspection/close','/api/permits/{id}/inspections','Full flow; TC_E2_02','PASS','Could Not Complete returns to issued/reschedulable state'),
 ('FR-09','In-app and SMS notifications','E2','NotificationService; SmsProvider','/api/notifications','TC_E2_03; workflow/queue tests','PARTIAL','In-app verified; SMS mock/fallback verified, no external delivery'),
 ('FR-10','Identity and action audit history','E3','AuditService; V8 migration','/api/audit; permit history','TC_E3_01; TC_E3_08','PASS','PostgreSQL triggers and API restrictions; superuser can disable protections'),
 ('FR-11','Workload and reference analytics','E4','AssignmentEngine.report; stage_reference.py','/api/reports/workload; /api/reference/analytics','TC_E4_05/08; staging tests','PASS','Reference provenance unverified; ambiguous records quarantined'),
 ('NFR-01','50 users with 95 percent within 3 seconds','All','API and persistence','All','Not executed','NOT TESTED','Concurrency correctness tests are not a 50-user load benchmark'),
 ('NFR-02','Role-based security','E1','SecurityConfig; controller/service guards','Protected API','Eight-role and E4 endpoint tests','PASS','No generic Admin bypass for permit review/payment'),
 ('NFR-03','Passwords, lockout and session security','E1','AuthSession; AuthService','/api/auth/*','SecurityIntegrationTest','PASS','29/30/31 minute idle, explicit activity, absolute limit and revocation'),
 ('NFR-04','9 of 10 users submit within 5 minutes','All','React forms','Applicant UI','Human protocol only','NOT TESTED','Recruit participants and record genuine outcomes'),
 ('NFR-05','Recovery within 10 seconds','E3','Transactions and database recovery','Persistence','TC_E3_07','PARTIAL','Rollback restoration measured; process crash/durability target unverified'),
 ('NFR-06','Required audit fields and protection','E3','audit_log; workflow_history; security_events','/api/audit','TC_E3_08; history assertions','PASS','Actor, action, date/time, entity and details retained'),
 ('NFR-07','Referential integrity','E3','Flyway FK constraints','Persistence','TC_E3_02; live schema validation','PASS','PostgreSQL and H2 invalid-reference checks'),
 ('NFR-08','Notification timing and fallback','E2','NotificationService','/api/notifications','TC_E2_03','PARTIAL','Synchronous local persistence verified; external timing not measured'),
]
write('REQUIREMENTS_TRACEABILITY_MATRIX.md','# PermitWorks requirements traceability\n\nLabels below paraphrase the supplied handbook and Gate 2 deck. The complete original Gate 1 wording was not supplied; no new acceptance criteria are invented. Engine ownership is unchanged. Evidence: FINAL_TEST_RESULTS.md, review_evidence/database/postgresql-engine-tests.log, integration logs and staging test log.\n\n| ID | Requirement | Engine | Code | API | Test | Status | Limitations |\n|---|---|---|---|---|---|---|---|\n'+'\n'.join('| '+' | '.join(r)+' |' for r in rtm))
defects=[
 ('D-00','Application could not compile','Stray trailing 0 after PermitApplication','Removed invalid token','baseline-backend.log','Compile and full verify passed'),
 ('D-01','Handbook reports expired JWT produced 401 instead of 403','Token issuance used test Clock; decoder used wall clock','JWT timestamp validator now uses the same injected Clock','Source finding; earlier 401 log was not supplied','All five support tests pass, including Applicant 403'),
 ('D-02','Short wrong password used request-length rejection','Password creation policy applied at login','Login keeps bounded nonblank input, credential failures return 401 and count toward lockout','Source/handbook finding; prior runtime sample not retained locally','Dedicated short-password test passes; creation bounds unchanged'),
 ('D-03','Stale test and migration documentation','README/VALIDATION reflected older runs','Generated counts from actual XML; document V1-V9 and database distinction','Prior VALIDATION.md and report','Updated validation and test register'),
 ('D-04','Idle timeout removed from code','Absolute-only sessions diverged from approved design','Additive V6 restores last_activity; explicit mutation activity; GET polling does not refresh','Preserved initial patch and source audit','Boundary/activity/absolute tests pass'),
 ('D-05','JPA context failed: missing application_versions.zone','Snapshot entity gained zone without migration column','Added snapshot zone to new V7 before live migration','iteration-2.log','Schema validation and engine tests pass'),
 ('D-06','11 security assertions failed on JWT expiry precision','JWT NumericDate uses seconds, test expected fractions','Use a fixed whole-second controlled test instant','iteration-3.log','Full security regression passes'),
 ('D-07','Live mutation returned 403 after CSRF cookie rotation','Frontend cached only the initial CSRF token','Read current XSRF cookie for each mutation; fetch when missing','review_evidence/defects/live-csrf-before-fix.log','Live API activity/logout and browser smoke retested'),
 ('D-08','Four historical completed review assignments counted as unfinished','New completion flag default did not reflect existing current-round reviews','Additive V9 backfills from review-round evidence and terminal state','review_evidence/database/legacy-workload-before.txt','TC_E3_09; live post-migration workload evidence'),
]
write('DEFECT_RETEST_REPORT.md','# PermitWorks defects and retests\n\nOriginal runtime failures are retained in review_evidence/defects or integration logs. Historical handbook failures are explicitly separated from reproduced failures. No failed test was removed to obtain a green run. Fixes are uncommitted working-tree changes.\n\n| ID | Failure | Root cause | Correction | Original evidence | Retest |\n|---|---|---|---|---|---|\n'+'\n'.join('| '+' | '.join(d)+' |' for d in defects)+'\n\nRetest commands: `mvn -B -Dpermit.build.directory=target-review verify`; `python scripts/local_review.py isolated-tests`; `python -m unittest discover -s scripts -p test_stage_reference.py -v`. Test-level execution times and outcomes are in FINAL_TEST_RESULTS.md. Local target cleanup stalled, so the configurable target-review output directory avoids stale generated classes without changing application behavior.')
livefile=ROOT/'review_evidence/database/live-schema.txt'
write('DATABASE_VERIFICATION.md',f'''# PermitWorks database verification

Baseline live database: PostgreSQL 17.11, localhost:5432/permits, trusted existing IntelliJ configuration. The existing runtime uses the postgres superuser; it should use a dedicated least-privilege runtime role before deployment. Credentials were not printed. Baseline: eight users, two permits, Flyway V1-V5 successful.

Backup: ignored backups/permits-before-sprint2-20261123-200131.dump, 52,284 bytes. No reset/drop/direct operational CSV import was performed.

V5 was already applied and is preserved unchanged. V6 backfills null last_activity from created_at and restores NOT NULL. Existing dormant sessions can require fresh login. V7 adds routing, notification, audit/document records and snapshot zone. V8 installs PostgreSQL append-only UPDATE/DELETE/TRUNCATE guards for audit_log, workflow_history and document_versions. V9 derives finished assignment flags from existing current-round reviews or terminal permits, preserving reopened-round work and all historical records. H2 deliberately skips PostgreSQL-specific trigger syntax.

Isolated PostgreSQL result: {len(pg)} tests, {g['PASS']} pass, {g['FAIL']} fail, {g['SKIP']} skip. Migration history V1-V9 and zero orphan assignments are recorded in review_evidence/database/isolated-integrity.txt. Test database names are retained in isolated-database.txt; no original records were used as failure-injection fixtures.

Live verification output: {'review_evidence/database/live-schema.txt' if livefile.exists() else 'PENDING final packaged backend startup and live recheck; do not claim migrated yet'}.

Reference data is staged separately in ignored reference_data/reference.sqlite. Password hashes, personal names/emails/addresses and document paths are excluded. Status mappings retain ambiguous REFERENCE categories; Inspection Officer is quarantined. Original ZIP SHA-256: {csv['archiveSha256']}. Provenance remains unverified. See reference_data/summary.json and invalid_records.json for counts and reasons.

Recovery scope: rollback is measured in an isolated transaction. No forced crash, power-loss durability or ten-second crash recovery result is claimed. Database superusers can disable triggers; these protections are not proof against a malicious superuser.
''')
write('ENGINE_READINESS.md',f'''# PermitWorks engine readiness

{summary}

| Engine | Owner | Demonstrable core | Evidence | Remaining limits |
|---|---|---|---|---|
| E1 | Akhil Teja | Login, eight roles, CSRF, lockout, idle/absolute expiry, revocation and Admin management | Security and support integration suites | TLS deployment/security attributes must be validated in deployment |
| E2 | Annapurna Boratti | Lifecycle, correction rounds, notifications, incomplete inspections, E4 assignment integration | Existing full-flow suite and TC_E2_01-03 | SMS mock; workflow/API design evolution requires guide review |
| E3 | Amruta Kutri | Transactions, concurrent writes, FK rejection, document versions and append-only audit | TC_E3_01-09 on PostgreSQL | Crash recovery and malicious-superuser protection not certified |
| E4 | Sanmathi Hegde | Eligible least-load routing, queue, confirmed override, SLA flags, reports and reference analytics | TC_E4_01-08; staging tests | Guide must approve actual profiles/SLA settings; no arbitrary deadlines supplied |

Core technical demonstrations are supported by automated tests. Final presentation/handbook approval, individual understanding, AI Mentor completion and upload cannot be certified by these tests. See FINAL_IMPLEMENTATION_REPORT.md for browser/live verification and final limitations.
''')
write('REVIEW_DEMO_GUIDE.md',r'''# PermitWorks local run and review demonstration

Run PowerShell from C:\Users\thele\Desktop\permit-engine1\permit-engine1. Use the existing private IntelliJ environment or set DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD and ADMIN_LOCAL_PART locally. Never paste secrets into the review documents.

```powershell
Set-Location backend
mvn -B '-Dpermit.build.directory=target-review' verify
mvn spring-boot:run
# Or, after verify, run the packaged jar with the same environment:
java -jar target-review\permit-engine1-1.0.0.jar
```

The local helper can read the existing trusted configuration without displaying values:

```powershell
python scripts\local_review.py verify
python scripts\local_review.py start
# start launches the already-built jar; do not run it if port 8080 is occupied.
```

In another PowerShell window:

```powershell
Set-Location frontend
npm ci
npm run build
npm run dev
```

If the E: Node shim stalls on this machine, use the bundled Node executable to run `node_modules/vite/bin/vite.js build --configLoader runner` directly, as recorded in the build evidence. Open http://localhost:5173 using localhost consistently. Backend is http://localhost:8080.

Reference import is isolated:

```powershell
python scripts\stage_reference.py --archive 'C:\Users\thele\Downloads\permit_csv_files (1).zip'
python -m unittest discover -s scripts -p test_stage_reference.py -v
python scripts\local_review.py isolated-tests
```

For the live workflow, use isolated demonstration records rather than changing the two original permits. Admin creates the required role accounts with unique passwords. Department Officer configures matching routing profiles in Assignment and workload. Explicit * accepts all types/zones only when intended. Use a guide-approved optional SLA; no SLA means no invented overdue target.

Applicant creates a draft with a zone, uploads a document if needed, and submits. Department Officer verifies documents and selects Auto assign reviewer and zoning officer. Technical and zoning owners record recommendations. Senior Officer requests a correction; applicant makes a real edit or uploads a new document version and resubmits. Finish both reviews and approve. Finance records a simulated payment. Department Officer auto-assigns an inspector. Inspector can demonstrate Could Not Complete with a reason, reschedule, record final pass and close.

Show Notifications for the correct recipient, officer workload/queue, preserved history/document versions, and Admin audit history. Actual SMS is not sent. Overdue testing uses the isolated automated test with an expired configured deadline, not backdating an original permit.

Individual five-minute explanation: one minute responsibility/code, one minute interface and persistence dependency, one minute normal case, one minute invalid/failure case and correction, one minute readiness/limitations. Each student must rehearse personally. Guide approval, AI Mentor and Drive upload remain pending external actions.
''')
guides={
 'E1_AKHIL_TECHNICAL_GUIDE.md':('E1','Akhil Teja','AuthService, AuthSession, SessionFilter, SecurityConfig, AuthController and AdminService','Explain authentication versus authorization, BCrypt one-way verification, HttpOnly versus CSRF, JWT signature versus server revocation, five failures and 423 lock status. A short wrong password is 401; malformed input remains 400. Polling reads do not extend idle expiry. Show the 29/30/31-minute and eight-hour tests; never wait eight real hours in a demo.'),
 'E2_TECHNICAL_GUIDE.md':('E2','Annapurna Boratti','PermitService, PermitController, NotificationService, LocalSmsProvider and WorkflowEntities','Explain guarded state transitions, ownership/assignment checks, review-round preservation and real-change resubmission. Show normal full flow, an invalid transition, and Could Not Complete followed by rescheduling. E2 advances status after E4 assignment; SMS is a mock and in-app persistence still succeeds if the provider fails.'),
 'E3_TECHNICAL_GUIDE.md':('E3','Amruta Kutri','WorkflowRepositories, AuditService, DocumentController, Flyway V1-V9 and EngineIntegrationTest','Explain atomic transactions, pessimistic row locks versus version columns, foreign-key rejection and controlled concurrent operations. Show rollback after multiple writes, old document bytes, audit fields and PostgreSQL trigger rejection. Measured rollback latency is not forced-crash recovery or proof of power-loss durability.'),
 'E4_TECHNICAL_GUIDE.md':('E4','Sanmathi Hegde','AssignmentEngine, EngineController, RoutingProfiles, AssignmentMutexes, Workload React panel and stage_reference.py','Explain role/type/zone/stage eligibility, PriorityQueue ordering by active work then lowest ID, and database mutex serialization. Show least-loaded assignment, unmatched-zone queue/alert, confirmed override with previous/new officer, configured deadlines and report filters. Explain why CSV reference IDs and hashes are never operational credentials.'),
}
for name,(engine,owner,classes,body) in guides.items():write(name,f'''# {owner} {engine} technical demonstration

Responsibility and implementation: {classes}.

{body}

Dependencies: E1 supplies authenticated identity and permissions; E2 owns status changes; E3 owns transactional records; E4 owns officer allocation. Controllers must not trust browser-supplied actor or owner IDs. Use the source method names in FINAL_TEST_RESULTS.md to show a normal, boundary and failure case, then the original defect and retest evidence.

Five-minute demo: 0-1 responsibility and classes; 1-2 normal input/output; 2-3 invalid or unauthorized case; 3-4 data/history and corrective test; 4-5 dependencies and current limitations. Run commands are in REVIEW_DEMO_GUIDE.md.

Viva prompts: What invariant does your engine enforce? Which engine owns the next operation? What happens if persistence fails? How do two concurrent requests behave? Which claims are tested, mocked, or still unverified? Answer using the actual implementation and the linked test outcomes, rather than claiming every NFR is complete.
''')
write('FINAL_IMPLEMENTATION_REPORT.md',f'''# PermitWorks implementation report

Implemented 23 November 2026 in the existing repository. Engine ownership and original Git history are preserved. No remote push or local commit was created; mixed pre-existing changes remain available for review.

E1 now enforces 30-minute inactivity and eight-hour absolute expiry with a consistent testable clock, 401 credential failures and 423 lockout. E4 now owns eligibility and least-loaded selection, queue/alert, confirmed replacement, optional SLA deadlines, filtered workload and processing statistics. E2 calls E4, keeps workflow ownership, persists notifications, rejects unchanged resubmission and handles incomplete inspections without closing. E3 adds immutable document versions and audit records, with tested atomicity, FK and concurrency behavior and PostgreSQL evidence triggers.

React retains the existing design and adds workload/routing, queue/retry, notifications, audit, file versions and CSV analytics. The original archive is staged separately, preserving ambiguous mappings and excluding credentials/personal text. Reference count totals and quarantine reasons are in reference_data/summary.json and invalid_records.json.

{summary}

Additional checks: frontend production build passed; three staging tests passed. Genuine failure/retest logs are retained and sanitized. Final live schema/row-preservation output and browser/API smoke results belong under review_evidence/database and integration; inspect those files before claiming live readiness. Handbook and review presentation are editable copies under review_evidence/documents; original Downloads files are preserved.

Remaining limitations: actual routing/SLA choices and API/schema evolution need guide approval; no real SMS delivery, 50-user load certification, human usability study or forced-crash recovery/10-second certification; file magic/size checks do not replace malware scanning. Document bytes are stored as base64 in the protected database for this prototype. Local runtime currently uses a superuser and should use a dedicated restricted role before deployment. H2 does not exercise PostgreSQL trigger syntax. The old destructive demo-reset script conflicts with append-only protections and must not be used on the migrated database; use an isolated demonstration database instead.

External requirements remain pending: guide approval, each student's AI Mentor activity, designated Drive upload and individual rehearsal. See ENGINE_READINESS.md, DATABASE_VERIFICATION.md, FINAL_TEST_RESULTS.md, DEFECT_RETEST_REPORT.md and REQUIREMENTS_TRACEABILITY_MATRIX.md.
''')
write('VALIDATION.md',f'''# PermitWorks validation record

23 November 2026. {summary}

Commands and evidence:

- `mvn -B -Dpermit.build.directory=target-review verify`: review_evidence/integration/final-v9-verify.log. Executable jar: backend/target-review/permit-engine1-1.0.0.jar.
- `python scripts/local_review.py isolated-tests`: review_evidence/database/postgresql-engine-tests.log; actual PostgreSQL 17.11 in a separate retained database, V1-V9.
- Native bundled Node `node_modules/vite/bin/vite.js build --configLoader runner`: review_evidence/integration/frontend-final-build.log; 91 modules.
- `python -m unittest discover -s scripts -p test_stage_reference.py -v`: review_evidence/database/staging-tests.log; three passed.
- Live database and browser smoke records are linked from DATABASE_VERIFICATION.md and FINAL_IMPLEMENTATION_REPORT.md.

PASS applies to recorded assertions only. H2 is not used as proof of live PostgreSQL behavior. SMS is mocked. Crash recovery, 50-user load and human usability remain unverified. See FINAL_TEST_RESULTS.md for per-case outcomes and execution times.
''')

# Update handbook text/table slots in the original OOXML package, preserving all other parts.
NS={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'};W='{'+NS['w']+'}'
source=Path.home()/'Downloads/Sprint_2_Student_Handbook_filled.docx'
with zipfile.ZipFile(source) as z: parts={i.filename:(i,z.read(i.filename)) for i in z.infolist()}
doc=L.fromstring(parts['word/document.xml'][1]);tables=doc.findall('.//w:tbl',NS)
def settext(cell,text):
 ts=cell.findall('.//w:t',NS)
 if not ts:
  p=L.SubElement(cell,W+'p');r=L.SubElement(p,W+'r');ts=[L.SubElement(r,W+'t')]
 ts[0].text=str(text)
 for t in ts[1:]:t.text=''
def table(index,rows):
 t=tables[index];old=t.findall('w:tr',NS);template=copy.deepcopy(old[1]);
 for r in old[1:]:t.remove(r)
 for values in rows:
  r=copy.deepcopy(template)
  for c,value in zip(r.findall('w:tc',NS),values):settext(c,value)
  t.append(r)
def cellrows(index):return tables[index].findall('w:tr',NS)
for r in cellrows(0):
 cells=r.findall('w:tc',NS)
 if ''.join(t.text or '' for t in cells[0].findall('.//w:t',NS)).strip()=='Date':settext(cells[1],'23/11/2026')
commit='main, HEAD 41d92ea; working-tree implementation, not committed'
table(3,[['FR-01 / NFR-02,03','E1 / Akhil Teja','Idle + absolute sessions, consistent JWT Clock, roles, lockout','Security and support assertions pass','Tested'],['FR-02,04,05,06,07,08','E2 / Annapurna Boratti','Workflow, real-change corrections, incomplete inspections','Full flow and TC_E2_01/02 pass','Tested'],['FR-09 / NFR-08','E2','Persistent in-app inbox; SMS provider mock/fallback','TC_E2_03 passes','Partial: external SMS unverified'],['FR-10 / NFR-05,06,07','E3 / Amruta Kutri','Audit, documents, transactions, FK, concurrent updates','TC_E3_01-09; PostgreSQL tests','Core tested; crash recovery pending'],['FR-03,11','E4 / Sanmathi Hegde','Profiles, heap allocation, queue/override, deadline/report/analytics','TC_E4_01-08 and staging tests','Core tested; guide approves rules']])
table(4,[['E1','4.13a, sequence B, 4.19-4.21','E3 users/sessions','30-minute idle restored; 423 lockout. Cookie/CSRF contract needs guide review.'],['E2','4.17/4.18/4.20','E1 identity, E3 writes, E4 routing','Granular status/action API and payment status evolution documented; approval pending.'],['E3','4.15/4.16/4.19','All engines','Consolidated users, snapshots, audit streams plus audit_log and binary versions. Diagram reconciliation pending approval.'],['E4','4.13d/4.19','E1 authorization, E2 status, E3 data','Designed eligibility/heap/tie implemented; explicit profile/SLA configuration needs guide approval.']])
table(5,[['E1','Authentication and sessions','BCrypt, role guards, CSRF, idle/absolute expiry and revocation','AuthService; E1 guide; current working tree','Deployment HTTPS check'],['E2','Permit lifecycle','Correction rounds, decisions, payment, incomplete inspection and notifications','PermitService; E2 guide; current working tree','External SMS and guide review'],['E3','Integrity and versions','Transactional saves, row locks, FK, audit protection and retained document bytes','E3 guide; migrations V1-V9; current working tree','Crash recovery unverified'],['E4','Assignment and reporting','Profile eligibility, minimum workload, queue/override, configured SLA and filtered reports','AssignmentEngine; E4 guide; current working tree','Guide-approved routing/SLA choices']])
table(6,[['E1','Deny-by-default; failed-attempt counter; clock boundaries','29-min read succeeds; 30/31-min idle denied','Activity refresh cannot extend absolute limit'],['E2','Guarded state machine and version comparison','Unchanged correction rejected; edited round accepted','Could Not Complete cannot close'],['E3','Row locks, transactions, FK and versions','Concurrent writes admit one valid outcome','Injected exception leaves no partial records'],['E4','Eligibility filter + PriorityQueue(load, officer ID)','Least load selected, lowest ID wins tie','No eligible profile queues work and alerts department']])
table(7,[['Client to E1','POST /api/auth/login','Short incorrect password','401, failure count increments','Real isolated test'],['Client to E1','POST /api/auth/activity; GET /me','Controlled time and cookie','Activity extends idle; GET polling does not','Real isolated test'],['E2 to E4','automatic(id,type,actor)','Matching type/zone/stage profiles','Eligible least-loaded officer or unassigned reason','Real isolated test'],['Officer to E4','POST /permits/{id}/override/{type}','confirm=true, officerId, reason','Previous/new assignee and audit retained','Real isolated test'],['Manager to E4','GET /api/reports/workload','from, to, permitType filters','Actual filtered counts and SLA flags','Real isolated test'],['User to E2','GET /api/notifications; POST /{id}/read','Authenticated recipient','Other recipient cannot mark read','Real isolated test'],['E2 to SMS','SmsProvider.send','Workflow event','MOCK_NOT_SENT; failure does not block inbox','Mock external delivery'],['Applicant to E3','POST /permits/{id}/documents/files','Safe PDF fixture, two versions','Prior bytes and metadata retained','Real isolated test']])
table(8,[['E1','V6 session consistency','Restore required last_activity','Backfill legacy nulls; NOT NULL restored','Migration/schema tests; live metadata'],['E2','Correction resubmission','Require actual content change','No-change rejected; changed round preserved','TC_E2_01'],['E2','Incomplete inspection','Reason, no closure; reschedule','Reschedulable issued status','TC_E2_02'],['E3','Atomic failure','No partial writes','Permit/history/audit counts unchanged','TC_E3_01'],['E3','FK and concurrent writes','Invalid references blocked; one valid outcome','Assertions pass on PostgreSQL','TC_E3_02-06'],['E3','Append-only evidence','Reject audit mutation','PostgreSQL trigger rejects UPDATE','TC_E3_08'],['E4','Concurrent assignment','One active assignee per permit/stage','Database mutex and idempotent selection','TC_E4_06']])
table(9,[['Simultaneous changes','E3/E4','Global allocation mutex plus per-permit row locks','TC_E4_06; TC_E3_03/04/06'],['Rollback','E3','Failure injection inside isolated transaction','TC_E3_01/07'],['Authorization','E1/E4','Controller roles and service guards; recipient/owner filtering','Security suite; TC_E4_05; document/inbox tests'],['Document movement','E3','Limited PDF/PNG/JPEG bytes stored as immutable versions','TC_E3_05'],['Session security','E1','30-minute idle, eight-hour absolute, CSRF and revocation','SecurityIntegrationTest'],['Recovery scope','E3','Rollback latency measured; forced crash not tested','Actual ROLLBACK_RECOVERY_MS log; NFR-05 partial']])
table(10,[[e,'See REVIEW_DEMO_GUIDE.md; mvn -B -Dpermit.build.directory=target-review verify',summary if e=='E1' else 'Engine assertions pass; see FINAL_TEST_RESULTS.md',commit] for e in ['E1','E2','E3','E4']]+[['Frontend','Native Node Vite build; npm run dev','Production bundle passed, 91 modules',commit]])
ids={};counters={}
for r in cases:
 match=re.match(r'TC_(E[234])_(\d+)',r['method'])
 if match:identifier=f'TC-{match[1]}-{match[2]}'
 else:
  prefix='TC-X' if r['class']=='SupportIntegrationTest' else 'TC-E1' if r['engine']=='E1' else 'TC-E2-FLOW' if r['engine']=='E2' else 'TC-E3-RESET'
  counters[prefix]=counters.get(prefix,0)+1;identifier=f'{prefix}-{counters[prefix]:02d}'
 ids[(r['class'],r['method'])]=identifier
(OUT/'handbook-test-id-map.json').write_text(json.dumps([{'handbookId':value,'class':key[0],'method':key[1]} for key,value in ids.items()],indent=2),encoding='utf-8')
table(11,[[ids.get((r['class'],r['method']),r['method'][:12]),r['engine']+' / '+r['database'],r['input'] if r['class']=='EngineIntegrationTest' else 'Isolated fixture; exact input in the linked test register',r['expected'],r['status']+f" in {r['seconds']:.3f}s",r['status']] for r in cases+pg])
for row,r in zip(cellrows(11)[1:],cases+pg):
 prefix=r['method'][:10]
 requirement='FR-03' if prefix.startswith('TC_E4') else 'NFR-05/07' if prefix.startswith('TC_E3') else 'FR-04/08/09' if prefix.startswith('TC_E2') else 'FR-01' if r['class']=='SecurityIntegrationTest' else 'Extra' if r['class'] in ['SupportIntegrationTest','ResetScriptTest'] else 'NFR-02' if r['class']=='AccessPolicyTest' else 'FR-02-08'
 settext(row.findall('w:tc',NS)[1],requirement+' / '+r['database'])
table(12,[['H2 full suite','mvn -B -Dpermit.build.directory=target-review verify','review_evidence/integration/final-v9-verify.log','23/11/2026; working tree'],['PostgreSQL engines','python scripts/local_review.py isolated-tests','review_evidence/database/postgresql-engine-tests.log','23/11/2026; isolated database'],['Frontend and staging','Native Vite build; Python unittest','frontend-final-build.log; staging-tests.log','23/11/2026'],['Per-test register','Surefire XML results extracted','FINAL_TEST_RESULTS.md; documents/test-results.json','Actual outcomes and elapsed seconds'],['Browser/live check','See integration and database evidence','API/browser smoke and live-schema records','No original permit edited']])
table(13,[[d[0],d[1],d[2]+'; '+d[3],'Working tree, not committed',d[5]] for d in defects])
table(14,[['E1','Authentication, authorization, lockout and sessions tested','HTTPS deployment check remains','50-user load not measured','Guide review and individual rehearsal'],['E2','Lifecycle, correction, inbox and incomplete inspection tested','External SMS mocked','External notification timing','Guide approval for API/state evolution'],['E3','Rollback, concurrent writes, FK, documents and audit tested','Superuser protection not claimed','Crash recovery and 10-second target','Restricted runtime DB role before deployment'],['E4','Heap allocation, queue, overrides, deadlines and reports tested','SLA is unset unless configured','Actual business routing/SLA choices need approval','Guide approval and individual rehearsal']])
table(15,[[r[0],r[2]+': '+r[3],r[5],r[6],r[7]] for r in rtm])
table(16,[['Week 10','Initial code and design handoff','Historical commit 41d92ea dated 26 October; source preserved'],['Week 11','Core increments','Do not invent individual authorship or unavailable weekly evidence'],['Week 12','Expanded checks and defects','Handbook describes 17 November run; its original log was not supplied locally'],['Week 13','Implementation and Gate 3 evidence','23 November: E4/E1/E2/E3 additions, genuine H2/PostgreSQL tests, staging/build and updated review pack']])
table(17,[['Design references and changes documented','Yes; guide approval pending'],['Each engine has a runnable tested core','Core assertions pass; individual rehearsal pending'],['Engine commands and actual evidence','Yes; REVIEW_DEMO_GUIDE and FINAL_TEST_RESULTS'],['Normal, boundary, failure tests','Yes; source method IDs and outcomes recorded'],['Concurrency / rollback / security','Yes; H2 and separate PostgreSQL tests'],['Defects and retests retained','Yes; actual failures separated from source findings'],['Git and complete traceability','Yes; current changes uncommitted; all FR/NFR statuses explicit'],['Mocks and limits declared','Yes; SMS, crash/load/usability and approval limits'],['Each student explains own engine','Pending personal demonstration'],['Guide approval, AI Mentor, Drive upload','Pending external actions; not claimed complete']])
summary_written=False
for para in doc.findall('.//w:body/w:p',NS):
 text=''.join(t.text or '' for t in para.findall('.//w:t',NS))
 if text.startswith('Full suite:') or text.startswith('Retest of TC-X-04') or text.startswith('Full log:'):
  if summary_written:para.getparent().remove(para)
  else:settext(para,summary+' See FINAL_TEST_RESULTS.md and retained logs.');summary_written=True
 elif 'Sprint2_test_evidence.txt' in text:settext(para,'Current evidence: FINAL_TEST_RESULTS.md; DEFECT_RETEST_REPORT.md; review_evidence/integration and database. Earlier referenced logs were not supplied locally.')
 elif 'SupportIntegrationTest.fix.patch' in text:settext(para,'Current correction is in source; see shared-Clock JWT validator and support retest evidence. No new commit claimed.')
 elif 'Repository paths above' in text:settext(para,'Source paths above refer to current working-tree implementation. HEAD 41d92ea is preserved; new changes are not committed.')
 elif text.startswith('Gate 2 handbook 4.22'):settext(para,'Current readiness: ENGINE_READINESS.md and individual technical guides. Earlier E4 Not started wording is superseded by tested implementation.')
 elif text.startswith('16. One-Page Example'):
  pr=para.find('w:pPr',NS)
  if pr is None:pr=L.SubElement(para,W+'pPr')
  L.SubElement(pr,W+'pageBreakBefore')
for t in tables:
 for row in t.findall('w:tr',NS):
  pr=row.find('w:trPr',NS)
  if pr is None:pr=L.Element(W+'trPr');row.insert(0,pr)
  if pr.find('w:cantSplit',NS) is None:L.SubElement(pr,W+'cantSplit')
previous=None
for para in doc.findall('.//w:body/w:p',NS):
 text=''.join(t.text or '' for t in para.findall('.//w:t',NS))
 if text.startswith('Current evidence:') and text==previous:para.getparent().remove(para)
 previous=text
newxml=L.tostring(doc,encoding='utf-8',xml_declaration=True,standalone=True)
destination=OUT/'SPRINT2_STUDENT_HANDBOOK_UPDATED.docx'
with zipfile.ZipFile(destination,'w',zipfile.ZIP_DEFLATED) as z:
 for name,(info,content) in parts.items():z.writestr(info,newxml if name=='word/document.xml' else content)
with zipfile.ZipFile(destination) as z:
 unchanged=all(z.read(name)==content for name,(info,content) in parts.items() if name!='word/document.xml')
assert unchanged
props=doc.findall('.//w:sectPr',NS)
(OUT/'artifact.md').write_text(f'''# Handbook template execution contract

Source: {source}
SHA-256: {hashlib.sha256(source.read_bytes()).hexdigest()}
Preserve original package parts except word/document.xml. Preserve source section geometry, title, ordered headings, styles, numbering, relationships, headers/footers and template example. Editable slots: tables 3-17 and stale evidence paragraphs; metadata date. Clone existing body row formatting for expanded records. Sections: {len(props)}.
Source PDF/render evidence: source-handbook.pdf and source-render/. Native Microsoft Word fallback used after packaged renderer failed because LibreOffice was absent. Final rendering must be inspected before delivery. All preserve-only package parts compare byte-for-byte equal: {unchanged}.
''',encoding='utf-8')
print(summary);print('Updated handbook:',destination);print('Preserve-only DOCX parts unchanged:',unchanged)
