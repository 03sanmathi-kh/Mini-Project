# PermitWorks registration implementation report

E1 owner Akhil Teja. Sprint 2 Review 4 Gate 3. Implemented and verified on 23 November 2026. Human guide approval is pending.

Full H2 regression: 77 executed, 76 passed, 0 failed, 1 skipped. PostgreSQL registration: 31 executed, 31 passed, 0 failed, 0 skipped. Frontend production build: PASS, 92 modules. Browser workflow: PASS.

The existing login now has one Register entry in the original button position. Registration, private status, Admin decision and independent private activation are integrated with the existing account model. Secondary Help and Support is preserved. Public personal email requests do not bypass @permitworks.com login or grant requested roles. Admin approval defaults to APPLICANT and requires confirmation. Activation consumes its independent secret and uses existing BCrypt and JWT login.

V10 is the only new registration migration. Existing V1–V9 were retained. Operational database verification confirms all original record signatures unchanged across users, permits, workflow history, audit, support, assignments and reviews. Eight users and two permits remain; browser fixtures are in a separate database. See evidence/live-v10-schema.txt and record-preservation.json.

New Java sources: RegistrationRequest, RegistrationRepositories, RegistrationDtos, RegistrationIdentityGuard, RegistrationRateLimiter, RegistrationService and RegistrationController. Existing AppUser, AuthService, AdminService, SecurityConfig and SessionFilter are integrated. UI adds Registration.jsx and updates main.jsx, api.js, style.css and development API target configuration. Tests add RegistrationIntegrationTest; scripts retain reproducible build/demo/evidence procedures.

API details: E1_API_DOCUMENTATION.md. Database: DATABASE_MIGRATION.md. Full test register: E1_TEST_CASE_REGISTER.md. Screenshots: screenshots/01 through 10, with tracking and activation credentials blurred. Browser checks: evidence/registration-browser-results.json. Modified tracked files (including preserved earlier working changes):

```text
.gitignore
INTEGRATION.md
README.md
VALIDATION.md
backend/pom.xml
backend/src/main/java/edu/kle/permits/AdminService.java
backend/src/main/java/edu/kle/permits/ApiErrors.java
backend/src/main/java/edu/kle/permits/AppUser.java
backend/src/main/java/edu/kle/permits/AuthController.java
backend/src/main/java/edu/kle/permits/AuthService.java
backend/src/main/java/edu/kle/permits/AuthSession.java
backend/src/main/java/edu/kle/permits/Dtos.java
backend/src/main/java/edu/kle/permits/PermitDtos.java
backend/src/main/java/edu/kle/permits/PermitService.java
backend/src/main/java/edu/kle/permits/SecurityConfig.java
backend/src/main/java/edu/kle/permits/SessionFilter.java
backend/src/main/java/edu/kle/permits/SupportRequests.java
backend/src/main/java/edu/kle/permits/SupportService.java
backend/src/main/java/edu/kle/permits/WorkflowEntities.java
backend/src/main/resources/application.yml
backend/src/test/java/edu/kle/permits/SecurityIntegrationTest.java
backend/src/test/java/edu/kle/permits/SupportIntegrationTest.java
frontend/src/Permit.jsx
frontend/src/api.js
frontend/src/main.jsx
frontend/src/style.css
frontend/vite.config.js

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

Final artifact QA: the updated handbook rendered through native Microsoft Word to19 pages; all pages were visually inspected. The revised28-slide presentation was rendered and checked for fit; package/layout/import verification is recorded in review_evidence/integration/deck-validation-registration-v2.json. Original Downloads files remain untouched.
