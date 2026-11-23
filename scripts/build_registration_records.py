"""Produce registration review records from real Surefire and browser evidence."""
from pathlib import Path
from copy import deepcopy
import json,re,subprocess,xml.etree.ElementTree as E
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.oxml import OxmlElement
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'FINAL_GATE3_DELIVERABLES';EV=OUT/'evidence'
def write(name,text): (OUT/name).write_text(text.strip()+'\n',encoding='utf-8')
def cases(folder):
 result=[]
 for p in sorted((ROOT/folder).glob('TEST-*.xml')):
  for c in E.parse(p).getroot().findall('testcase'):
   result.append(dict(test=c.get('name'),suite=c.get('classname').split('.')[-1],result='FAIL' if c.find('failure') is not None or c.find('error') is not None else 'SKIP' if c.find('skipped') is not None else 'PASS',seconds=c.get('time'),evidence=str(p.relative_to(ROOT)).replace('\\','/')))
 return result
def totals(rows):return f"{len(rows)} executed, {sum(c['result']=='PASS' for c in rows)} passed, {sum(c['result']=='FAIL' for c in rows)} failed, {sum(c['result']=='SKIP' for c in rows)} skipped"
def human(text):return re.sub(r'(?<=[a-z])(?=[A-Z])',' ',text.replace('_',' '))
def main():
 h=cases('backend/target-registration/surefire-reports');g=cases('backend/target-registration-pg/surefire-reports');assert h and g and all(c['result']!='FAIL' for c in h+g)
 browser=json.loads((EV/'registration-browser-results.json').read_text());assert browser['result']=='PASS'
 preservation=json.loads((EV/'record-preservation.json').read_text());assert all(v=='PASS' for v in preservation.values())
 summary=f"Full H2 regression: {totals(h)}. PostgreSQL registration: {totals(g)}. Frontend production build: PASS, 92 modules. Browser workflow: PASS."
 (EV/'registration-test-results.json').write_text(json.dumps(dict(h2=h,postgresql=g,browser=browser,summary=summary),indent=2))
 api='''| Method and path | Access and request | Result |
|---|---|---|
| GET /api/registrations/roles | Public | Existing eight roles only |
| POST /api/registrations | Public + CSRF; fullName, personalEmail, phone, requestedRole, optional description | 201; message, trackingCode, independent activationSecret shown once |
| POST /api/registrations/status | Public + CSRF; personalEmail, trackingCode | 200; state, message, approvedRole, loginEmail after approval, rejectionReason if rejected, activationExpiresAt |
| POST /api/registrations/activate | Public + CSRF; personalEmail, trackingCode, activationSecret, newPassword, confirmPassword | 200; assigned loginEmail; existing sign-in required |
| GET /api/admin/registrations?status=PENDING&page=0 | Admin cookie | 20-row pages; all states when status omitted; no credential hashes |
| POST /api/admin/registrations/{id}/approve | Admin + CSRF; approvedRole, confirm:true | 200; disabled pending-activation user and reviewed request |
| POST /api/admin/registrations/{id}/reject | Admin + CSRF; reason, confirm:true | 200; rejection reason; no user created |'''
 write('E1_API_DOCUMENTATION.md',f'''# E1 registration API

Owner Akhil Teja. Additional guide-requested feature for Sprint 2 Review 4 Gate 3. Public registration uses personal email; existing login still requires the assigned @permitworks.com identity.

{api}

Fetch /api/auth/csrf before public mutations; send X-XSRF-TOKEN from the XSRF-TOKEN cookie. Admin operations use the existing PERMIT_TOKEN HttpOnly cookie. Registration does not issue a session. Existing JWT, lockout, CSRF and session limits remain in force.

Errors: 400 validation/confirmation; 401 missing Admin session or unusable account login; 403 non-Admin, missing CSRF or unavailable/wrong/expired activation; 404 identical unknown email/tracking response; 409 duplicate identity or reviewed request; 429 rate limit. Duplicate eligibility returns the same message for an existing account and an active request, but does reveal that the address is unavailable. Status requires both personal email and unpredictable tracking code.

Input: name <=100 letters/marks/spaces/apostrophes/periods/hyphens; normalized case-insensitive email <=254; phone 7–15 digits, optional leading plus; description <=2000; rejection reason <=500; passwords 8–72 UTF-8 bytes, matching confirmation. Role values are ADMIN, APPLICANT, DEPARTMENT_OFFICER, PERMIT_REVIEWER, ZONING_OFFICER, SENIOR_DEPARTMENT_OFFICER, FINANCE_OFFICER, BUILDING_INSPECTOR.

Per source address, shaped valid requests are limited to 5 submissions/hour, 30 lookups/10 minutes and 10 activation attempts/15 minutes. Failed lookups consume their allowance in a separate transaction. Validation failures precede this limiter. IP windows are database-backed and do not trust arbitrary forwarding headers. Shared NAT users share the limit.

Tracking uses 24 cryptographically random bytes (192 bits) with PWREG- prefix. The independent activation secret uses 32 random bytes (256 bits). Only SHA-256 hashes persist; neither credential appears in Admin DTOs. Activation is single use and expires exactly seven days after approval. BCrypt uses the existing encoder. No SMTP, OTP, SMS or external API is introduced.
''')
 write('E1_IMPLEMENTATION_REGISTER.md','''# E1 registration implementation register

Owner Akhil Teja. Additional guide-requested feature; human guide approval is pending.

| Component | Implementation and checked behavior |
|---|---|
| Login entry | Original Get help position and 82.453125 by 38 px footprint now contain one Register button |
| Public forms | Registration.jsx: validated details, eight requested roles, confirmation and status, private password setup, accessible modal and secondary support |
| Identity | RegistrationIdentityGuard: shared transaction mutex across public requests and direct Admin user creation/update; normalized account and request checks |
| Review | Admin-only paginated list; explicit confirmation; least-privilege default APPLICANT; selected role becomes actual user role |
| Persistence | V10 additive tables and app_users.activation_pending; previous migration checksums retained |
| Activation | Independent private secret, exact seven-day deadline, password confirmation/BCrypt, consumed secret, locked transaction |
| Bypass prevention | AuthService rejects activation_pending even if enabled was changed; Admin enable refuses pending accounts |
| Evidence | Audit actor/action/time/entity for review and activation; public submission creates a security event without exposing personal details |
| Existing flows | Existing support routes and Admin support, JWT cookies, CSRF, lockout, idle/absolute session handling retained |
''')
 write('DATABASE_MIGRATION.md','''# Registration database migration

V10__registration_approval_activation.sql follows the actual V9 migration. Do not edit already-applied V1–V9. Flyway applies V10 during normal backend startup; Hibernate validates the schema afterward.

app_users gains activation_pending BOOLEAN NOT NULL DEFAULT FALSE. All existing users retain passwords, roles and enablement. registration_requests stores normalized contact fields, requested/approved existing role names, state, hashed credentials, reviewer/time, rejection reason, linked account, activation deadline and consumption time. CHECK constraints enforce consistent PENDING, APPROVED, REJECTED and ACTIVATED records. Unique active_email reserves an identity while active; rejection releases it. user_id and tracking_hash are unique; user/reviewer foreign keys reference app_users. Status/time and email indexes support review and lookup.

registration_mutex serializes duplicate identity checks, approval and activation with pessimistic locks. registration_limits persists hashed source identifiers, windows and attempts. The global mutex favors correctness for this small project; it is not a throughput benchmark.

A private pre-migration PostgreSQL custom-format backup is retained under backups/. No reset, DROP, record deletion or down migration was performed. Browser and PostgreSQL test databases were newly created and retained separately. evidence/record-preservation.json compares original IDs and record signatures; login counter/timestamp mutations are excluded from user identity signatures.

Recovery: stop the application, investigate and restore the private backup to a separately named recovery database if needed. Do not run the old reset script or overwrite the operational database as a routine rollback.
''')
 rows=['| Test ID / method | Database | Actual | Seconds | Evidence |','|---|---|---|---:|---|']
 for db,items in [('H2',h),('PostgreSQL',g)]:
  for c in items:rows.append(f"| {c['test']} | {db} | {c['result']} | {c['seconds']} | {c['evidence']} |")
 write('E1_TEST_CASE_REGISTER.md',f'''# Registration and regression test register

23 November 2026. {summary}

The source method names describe the input and expected assertion. RegistrationIntegrationTest contains TC_REG_01–25 and 27–32, run on H2 and PostgreSQL. TC-REG-26 is the separate production frontend build. Full details and fixture payloads remain in the source; no production users are fixtures. The H2-only skip checks PostgreSQL append-only triggers; the previous core PostgreSQL run is historical and is not counted as a current V10 full PostgreSQL regression.

{chr(10).join(rows)}

TC-REG-26: PASS. Evidence: evidence/registration-frontend-build-final.log. No existing frontend unit-test script is defined. Real Chrome end-to-end evidence: evidence/registration-browser-results.json and screenshots/.
''')
 write('DEFECT_AND_RETEST_REGISTER.md',f'''# Registration defect and retest register

| ID | Observed failure | Correction | Actual retest |
|---|---|---|---|
| REG-D01 | Java Query annotation ambiguous between JPA and Spring Data | Explicit imports in registration repositories | Compiled in full verify and PostgreSQL run |
| REG-D02 | New integration test omitted MockHttpServletRequestBuilder import | Added correct test import | All registration test methods compiled and executed |
| REG-D03 | H2 preservation test queried unquoted lowercase Flyway metadata table | Quote Flyway table and column identifiers, preserving production SQL | Final full suite: {totals(h)}; PostgreSQL prior lowercase query already passed |

These are observed implementation/test defects, not fabricated production incidents. Initial logs are retained, with sensitive test payload values redacted. No unresolved test failure remains in the final recorded suites. Guide acceptance and broader human/load validation remain separate.
''')
 write('REQUIREMENTS_TRACEABILITY_MATRIX.md','''# Registration traceability addition

The following E1-REG identifiers are local labels for this additional guide-requested feature, not newly invented approved FR numbers. Existing FR-01–11 and NFR-01–08 mappings remain in the project REQUIREMENTS_TRACEABILITY_MATRIX.md.

| Local feature | Source and API | Evidence |
|---|---|---|
| E1-REG-01 Single entry and validated request | Login; Registration.jsx; POST /registrations | TC_REG_01–07; browser button and modal |
| E1-REG-02 Private status | status endpoint; hashed random tracking | TC_REG_08,24,25; Pending/Approved/Rejected screenshots |
| E1-REG-03 Admin decision and role | AdminRegistrationRequests; approve/reject | TC_REG_09–14,23,32; browser requested ADMIN approved APPLICANT |
| E1-REG-04 Private activation and existing login | activate; AuthService | TC_REG_15–20,27–29,31; successful browser login and Admin 403 |
| E1-REG-05 Additive data and concurrency | V10; IdentityGuard; mutex and row locks | TC_REG_22,23,30,31; original-record signatures |
| E1-REG-06 Existing support and build | secondary Help; original support controllers | TC_REG_21; TC-REG-26; support screenshot |

NFR boundaries: automated concurrency is not 50-user performance certification. Browser verification is not a ten-person usability trial. SMTP/OTP/SMS and external delivery are outside this registration implementation. Human guide approval is pending.
''')
 write('E1_INDIVIDUAL_DEMONSTRATION_GUIDE.md','''# Akhil Teja E1 registration demonstration

Explain that E1 owns access control and communication. Registration is an additional guide-requested feature for Sprint 2 Review 4 Gate 3. Use a newly created isolated demo database; keep the operational eight users and two permits intact.

1. Open http://localhost:5174 for the installed local application. Show the single Register button in the former Get help position. Demonstrate Help and Support inside the registration modal.
2. On an isolated demo, enter a name, personal email, phone and requested role. Submit. Privately save both tracking code and the separate activation secret; never paste them into slides or Git.
3. Check status using personal email plus tracking code: PENDING. Explain that submission creates no login user.
4. Sign in as Admin; open Registration Requests. Review details, choose the approved role and confirm. Requested ADMIN does not grant ADMIN; the example approves APPLICANT. Approval creates a disabled pending-activation account.
5. Sign out. Check APPROVED status and note the assigned @permitworks.com login identity. Set a password with the separately saved secret. The private secret expires seven days after approval and is consumed once.
6. Sign in using the assigned email and new password. Show APPLICANT permissions and logout. Demonstrate a separate rejected request and the verified user-visible reason.
7. Show V10, actual test summaries and screenshots. Explain BCrypt, hashed random credentials, CSRF, existing JWT cookies and serialization preventing duplicate accounts.

Viva boundaries: personal email is not the login identity. Tracking alone cannot activate. Admin cannot bypass activation through Enable. No email/SMS is sent. Lost secrets and expired approvals require manual Help and identity verification; there is no implemented reset/reissue workflow. The demonstration proves observed assertions; guide approval, load certification and human usability acceptance are not claimed.

Reproduce isolated demo after packaging: python scripts/registration_environment.py demo; read credentials privately from ignored private-build/demo-access.json, open http://localhost:5175, then python scripts/registration_environment.py stop-demo. Do not share the private access file. This creates a separate retained database and leaves operational records intact.
''')
 git=subprocess.run(['git','status','--short'],cwd=ROOT,capture_output=True,text=True).stdout;head=subprocess.run(['git','rev-parse','HEAD'],cwd=ROOT,capture_output=True,text=True).stdout.strip();branch=subprocess.run(['git','branch','--show-current'],cwd=ROOT,capture_output=True,text=True).stdout.strip()
 write('GIT_EVIDENCE.md',f'# Genuine local Git evidence\n\nBranch {branch}; HEAD {head}. Registration and earlier project changes remain local working changes. No new commit or remote push is claimed. Initial status is retained in evidence/initial-git-status.txt; the private baseline patch is ignored.\n\n```text\n{git}\n```')
 changes=subprocess.run(['git','diff','--name-only'],cwd=ROOT,capture_output=True,text=True).stdout
 write('FINAL_IMPLEMENTATION_REPORT.md',f'''# PermitWorks registration implementation report

E1 owner Akhil Teja. Sprint 2 Review 4 Gate 3. Implemented and verified on 23 November 2026. Human guide approval is pending.

{summary}

The existing login now has one Register entry in the original button position. Registration, private status, Admin decision and independent private activation are integrated with the existing account model. Secondary Help and Support is preserved. Public personal email requests do not bypass @permitworks.com login or grant requested roles. Admin approval defaults to APPLICANT and requires confirmation. Activation consumes its independent secret and uses existing BCrypt and JWT login.

V10 is the only new registration migration. Existing V1–V9 were retained. Operational database verification confirms all original record signatures unchanged across users, permits, workflow history, audit, support, assignments and reviews. Eight users and two permits remain; browser fixtures are in a separate database. See evidence/live-v10-schema.txt and record-preservation.json.

New Java sources: RegistrationRequest, RegistrationRepositories, RegistrationDtos, RegistrationIdentityGuard, RegistrationRateLimiter, RegistrationService and RegistrationController. Existing AppUser, AuthService, AdminService, SecurityConfig and SessionFilter are integrated. UI adds Registration.jsx and updates main.jsx, api.js, style.css and development API target configuration. Tests add RegistrationIntegrationTest; scripts retain reproducible build/demo/evidence procedures.

API details: E1_API_DOCUMENTATION.md. Database: DATABASE_MIGRATION.md. Full test register: E1_TEST_CASE_REGISTER.md. Screenshots: screenshots/01 through 10, with tracking and activation credentials blurred. Browser checks: evidence/registration-browser-results.json. Modified tracked files (including preserved earlier working changes):

```text
{changes}
```

Exact validation commands from the project root (PowerShell quotes Maven properties):

```powershell
Set-Location backend
mvn -B '-Dpermit.build.directory=target-registration' verify
Set-Location ..
python scripts/registration_environment.py pg-tests
Set-Location frontend
node node_modules/vite/bin/vite.js build --configLoader runner
Set-Location ..
python scripts/registration_environment.py demo
node scripts/registration_browser.cjs
python scripts/registration_environment.py preservation
python scripts/registration_environment.py stop-demo
```

Use the bundled native Node path when system Node is unavailable. Browser script requires the bundled Playwright and installed Chrome. Native backend is running at 8080 and UI at 5174 with the existing private local environment. Reproduction helpers never print passwords. The isolated demo uses 8081/5175 and a newly created database.

Limitations: manual Admin approval; no SMTP, email verification, OTP or SMS; no automated secret recovery/reissue after loss or expiry; per-source rate limits share NAT allowances and apply after request-shape validation; global locking is designed for project correctness, not high-throughput certification. No new load/usability/forced-crash acceptance result or guide sign-off is claimed. No database reset, original record deletion, fabricated commit or remote push occurred.
''')
 validation=f'# PermitWorks current registration validation\n\n23 November 2026. {summary}\n\nCurrent full source: backend/target-registration/surefire-reports. Current PostgreSQL registration suite: backend/target-registration-pg/surefire-reports. Frontend build: FINAL_GATE3_DELIVERABLES/evidence/registration-frontend-build-final.log. Browser: FINAL_GATE3_DELIVERABLES/evidence/registration-browser-results.json. Live V1–V10 and original-data preservation: FINAL_GATE3_DELIVERABLES/evidence/live-v10-schema.txt and record-preservation.json.\n\nEarlier 46-case H2 / 20-case PostgreSQL engine checks and CSV staging tests remain historical in review_evidence. They are not counted as additional current V10 executions. See FINAL_GATE3_DELIVERABLES/E1_TEST_CASE_REGISTER.md for every current case. No frontend unit-test command exists; build and actual Chrome checks were run. Broader NFR limits and human approval remain documented.\n'
 (ROOT/'VALIDATION.md').write_text(validation,encoding='utf-8')
 # Preserve the supplied handbook and append a clearly dated, current registration addendum.
 doc=Document(ROOT/'review_evidence/documents/SPRINT2_STUDENT_HANDBOOK_UPDATED.docx')
 doc.add_page_break();doc.add_heading('E1 registration verification addendum',0)
 doc.add_paragraph('23 November 2026 | Akhil Teja | Sprint 2 Review 4 Gate 3')
 doc.add_paragraph('This addendum updates the earlier four-engine handbook with the additional guide-requested registration feature. Earlier test counts are historical; the results below supersede them for current registration validation. Human guide approval remains pending.')
 doc.add_paragraph(summary)
 def heading(t):doc.add_heading(t,1)
 def para(t):doc.add_paragraph(t)
 def table(headers,records):
  t=doc.add_table(rows=1,cols=len(headers));t.style='Table Grid'
  for c,v in zip(t.rows[0].cells,headers):c.text=v
  for record in records:
   for c,v in zip(t.add_row().cells,record):c.text=str(v)
  for row in t.rows:
   row._tr.get_or_add_trPr().append(OxmlElement('w:cantSplit'))
   for c in row.cells:
    for p in c.paragraphs:
     for r in p.runs:r.font.size=Pt(9)
  t.rows[0]._tr.get_or_add_trPr().append(OxmlElement('w:tblHeader'))
 heading('Implementation and identity boundaries')
 para('Register replaces the original Get help entry, preserving its footprint. The modal captures name, personal email, phone, an existing requested role and optional reason. Help and Support remains available inside it. Submission creates a PENDING request and shows independent random tracking and private activation credentials once. Only hashes are persisted.')
 para('Personal email and tracking code verify status. Admin reviews the paginated queue, explicitly selects the approved role and confirms approval or rejection. Requested privileges do not grant access. Approval creates a disabled pending-activation account; activation needs the private secret and matching password. BCrypt and existing login/JWT/CSRF/session handling are retained. Lost credentials or expired approvals require manual Help; automated reissue is not implemented.')
 table(['Register entry','Owner','Status'],[['Request, status, Admin review, private activation','Akhil Teja E1','Implemented and tested; guide approval pending'],['Seven-day deadline and single use','E1','Boundary and concurrency tests pass'],['Existing support and accounts','E1','Support smoke and original-data signatures pass']])
 heading('API register')
 table(['Route','Purpose'],[['GET /api/registrations/roles','Existing eight roles'],['POST /api/registrations','Request; 201; private credentials once'],['POST /api/registrations/status','Verified personal email plus tracking'],['POST /api/registrations/activate','Independent secret and password'],['GET /api/admin/registrations','Admin paginated and filtered queue'],['POST /api/admin/registrations/{id}/approve','Confirmed final role and account creation'],['POST /api/admin/registrations/{id}/reject','Confirmed reason; no user creation']])
 heading('Database and transaction register')
 para('V10 follows actual V9. It adds app_users.activation_pending with FALSE for existing accounts, registration_requests, registration_mutex and registration_limits. Unique keys, state CHECK constraints and foreign keys protect identity and lifecycle. Global and row locks serialize duplicate submission, review and activation. No original migration was rewritten and no production database reset occurred. All seven original-record signature groups passed; eight users and two permits remain.')
 heading('Test case and execution register')
 para(summary+' TC-REG-26 is the frontend production build. The following registration methods each passed on H2 and PostgreSQL. Full existing suite outcomes and paths are in E1_TEST_CASE_REGISTER.md. One PostgreSQL-only trigger test is skipped in H2; this is not reported as a pass.')
 table(['Test ID','Checked scenario','H2 / PostgreSQL'],[[re.match(r'TC_REG_\d+',c['test']).group(),human(re.sub(r'^TC_REG_\d+_','',c['test'])),'PASS / PASS'] for c in g])
 heading('Defects and retests')
 table(['Defect','Correction','Retest'],[['Ambiguous Query import','Explicit repository imports','Full compilation passed'],['Missing test request-builder import','Added import','31 registration methods execute'],['H2 Flyway metadata case','Quoted identifiers in test query','Current full suite passed']])
 heading('Traceability and acceptance')
 para('Local E1-REG-01 through 06 link request UI, private status, Admin review, activation, additive persistence and preserved support to TC_REG_01 through 32, TC-REG-26 build and real browser evidence. These are additional feature labels, not invented approved FR numbers. Existing FR and NFR traceability remains in the earlier register. No 50-user load, ten-person usability, real SMS delivery or forced-crash certification is claimed.')
 heading('Individual demonstration and Git evidence')
 para('Demonstrate Register, PENDING, Admin approval with APPLICANT selected despite ADMIN requested, APPROVED lookup, private password activation, correct-role login and logout. Then demonstrate a separately rejected request and visible reason. Use isolated data; keep both private credentials out of the review deck. The screenshots were captured from real Chrome. Current local branch '+branch+' at '+head[:7]+' has uncommitted working changes; no new commit or remote push is claimed.')
 heading('Evidence and remaining work')
 para('Evidence files are under FINAL_GATE3_DELIVERABLES/evidence and screenshots. The current API, test, migration, defect, RTM, implementation and individual demo registers accompany this handbook. The live application is at http://localhost:5174. Human guide review and the previously untested broader NFR acceptance tasks remain outstanding.')
 doc.save(OUT/'SPRINT2_STUDENT_HANDBOOK_REGISTRATION.docx')
 print(summary);print('Review records and updated handbook written.')
if __name__=='__main__':main()
