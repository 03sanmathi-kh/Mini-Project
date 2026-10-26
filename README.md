# PermitWorks

PermitWorks is a presentation-ready, role-based building permit approval and inspection workflow. It follows a permit from applicant draft through document verification, assigned technical and zoning reviews, corrections, final decision, simulated payment, permit issuance, inspections, and project closure.

## Architecture and technology

The React 19 + Vite frontend calls a Spring Boot 3.5 REST API through Axios. Spring Security validates an HS256 JWT stored only in an HttpOnly, SameSite=Strict cookie; server-side session rows provide revocation, an eight-hour absolute lifetime, and a 30-minute idle timeout. Spring Data JPA persists normalized workflow records in PostgreSQL 17. Flyway owns all schema changes. OpenAPI/Swagger remains available to signed-in Administrators.

```text
Browser (React/Vite)
  -> CSRF-protected REST API (Spring MVC)
  -> role + ownership/assignment checks (Spring Security and PermitService)
  -> transactional state machine (Spring Data JPA)
  -> PostgreSQL schema (Flyway V1 through V4)
```

The workflow tables are `permit_applications`, `application_versions`, `application_assignments`, `application_reviews`, `correction_requests`, `payment_records`, `inspections`, and append-only-through-the-API `workflow_history`. `V4__personal_email_and_support.sql` adds personal contact email and the normalized `support_requests` workflow without changing an existing account password hash.

## Quick start with Docker Desktop

Install Docker Desktop with Linux containers, then run from this directory:

```bat
start-windows.bat
```

On first run, `setup.ps1` creates an untracked `.env` with random database, JWT, and Admin passwords. Open <http://localhost:5173> and sign in as `admin@permitworks.com` with the generated `ADMIN_PASSWORD`.

Stop without removing records:

```bat
docker compose down
```

Never run `docker compose down -v` unless you intentionally want to delete the PostgreSQL volume.

## Native and IntelliJ startup

Prerequisites: JDK 17+, Maven 3.6.3+, Node 22.12+, and PostgreSQL. Create database `permits` and a migration-capable database user.

Set these environment variables in the IntelliJ Spring Boot run configuration for `edu.kle.permits.PermitApplication`:

```text
DB_URL=jdbc:postgresql://localhost:5432/permits
DB_USER=permits
DB_PASSWORD=<database password>
JWT_SECRET=<random secret of at least 32 UTF-8 bytes>
ADMIN_LOCAL_PART=admin
ADMIN_PASSWORD=<initial password, 8-72 UTF-8 bytes>
ALLOWED_ORIGIN=http://localhost:5173
SECURE_COOKIE=false
```

Run the backend from IntelliJ or:

```bat
cd backend
mvn spring-boot:run
```

In another terminal:

```bat
cd frontend
npm ci
npm run dev
```

Use `localhost` consistently. For HTTPS deployment set `SECURE_COOKIE=true`, set `ALLOWED_ORIGIN` to the exact UI origin, and terminate TLS at the reverse proxy.

## Account emails and passwords

- Every account has a unique PermitWorks login email ending in `@permitworks.com`. It is the **only** login identifier. Admin normally enters only a local part; the backend appends the domain, trims it, and normalizes it to lowercase.
- Personal contact email is a separate, ordinary email address used only for contact and support. It is normalized to lowercase, is never accepted for authentication, and is not added to JWT identity claims. Migrated accounts may have a null personal email until Admin edits them.
- Login requires the complete PermitWorks address, such as `amruta@permitworks.com`.
- The server trims and lowercases identifiers and rejects empty local parts, unsupported characters, any `@` in the local-part field, external domains, and duplicates.
- `V2__permitworks_email.sql` renames the existing `username` column and converts records such as `admin` and `applicant1` to `admin@permitworks.com` and `applicant1@permitworks.com`. User IDs, BCrypt hashes, roles, sessions, and security-event foreign keys remain unchanged.
- New and reset passwords must contain **8-72 UTF-8 bytes**. This is a byte limit, not a character limit, so non-ASCII characters may use more than one byte. BCrypt hashing remains unchanged and existing hashes are never rewritten during migration or startup.

### Creating a user

1. Sign in as Admin and open **User management**.
2. Enter full name, PermitWorks local part, personal contact email, role, and an initial 8-72-byte password.
3. Confirm the generated `local-part@permitworks.com` address and assigned role before saving.
4. After creation, securely communicate the generated PermitWorks email and initial password to the user manually. PermitWorks does not retrieve existing passwords and does not pretend to send email or SMS.

## Roles

| Role | Allowed workflow responsibilities |
|---|---|
| ADMIN | Manage users, unlock accounts, view security activity, monitor all permits read-only |
| APPLICANT | Create/edit owned drafts or corrections, submit/resubmit, view owned history |
| DEPARTMENT_OFFICER | Verify documents, request missing documents, assign role-qualified officers, monitor workflow |
| PERMIT_REVIEWER | View and review only assigned technical work; approve recommendation or request correction |
| ZONING_OFFICER | View and review only assigned zoning work; approve recommendation or request correction |
| SENIOR_DEPARTMENT_OFFICER | View review-complete work; approve, reject, or request correction |
| FINANCE_OFFICER | View approved/payment-complete work; record fee and simulated payment |
| BUILDING_INSPECTOR | View only assigned permits; schedule and result inspections; close after final pass |

All other actions are denied. Admin deliberately has no review, decision, payment, or inspection privilege. UI button hiding is only a convenience; controller and service checks remain authoritative.

## Workflow state diagram

```text
DRAFT -> SUBMITTED -> DOCUMENT_VERIFICATION -> ASSIGNED
  -> TECHNICAL_REVIEW -> ZONING_REVIEW -> PENDING_FINAL_DECISION
  -> APPROVED -> PAYMENT_PENDING -> PERMIT_ISSUED
  -> INSPECTION_SCHEDULED -> INSPECTION_PASSED -> PROJECT_CLOSED
                              |-> INSPECTION_FAILED -> schedule again

Document, technical, zoning, or final-decision correction
  -> CORRECTION_REQUIRED -> RESUBMITTED -> required reviews again

Final decision may also end at REJECTED.
```

Invalid transitions return HTTP 409 with a useful message. Request validation returns 400, missing authentication 401, role/ownership/assignment violations 403, and an inaccessible or absent permit 404. Workflow mutations lock the permit row and run in a database transaction. Submitted application snapshots, historical assignments, completed reviews, corrections, inspections, and workflow history are never overwritten.

## Main API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/login` | Complete-email login |
| POST | `/api/support` | Public support-request submission |
| POST | `/api/support/status` | Public ticket check using ticket number and personal email |
| GET / PATCH | `/api/admin/support`, `/api/admin/support/{id}` | Admin-only support management |
| GET | `/api/permits/dashboard` | Role-filtered counts and application list |
| POST / PATCH | `/api/permits`, `/api/permits/{id}` | Create or edit an applicant draft/correction |
| POST | `/api/permits/{id}/submit`, `/resubmit` | Applicant submission actions |
| POST | `/api/permits/{id}/documents` | Department document decision |
| POST | `/api/permits/{id}/assignments` | Role-checked assignment/reassignment |
| POST | `/api/permits/{id}/technical-review`, `/zoning-review` | Immutable assigned review |
| POST | `/api/permits/{id}/decision` | Senior decision |
| POST | `/api/permits/{id}/payment` | Fee/payment and permit issuance |
| POST | `/api/permits/{id}/inspections` | Schedule inspection |
| POST | `/api/permits/{id}/inspections/{inspectionId}/result` | Assigned inspector result |
| POST | `/api/permits/{id}/close` | Close after successful final inspection |

All mutating calls require the CSRF cookie/header pair. The frontend initializes and sends it automatically. Swagger UI is at <http://localhost:5173/swagger-ui/index.html> after Admin login.

## Help and support

From the sign-in page, select **Get help** to report a forgotten password or PermitWorks email, locked/disabled account, unrecognized email, new-account need, login problem, or another issue. Full name, personal contact email, category, subject, and description are required; the PermitWorks email is optional. Submissions are validated, rate-limited per source IP, stored in PostgreSQL, and return a human-readable ticket such as `PW-HELP-2026-00001`. The public response never reveals whether an account exists.

Use **Check ticket status** with the ticket number and the same personal contact email. The public result contains only category, status, submitted/updated times, and any user-visible response. It never contains Admin notes, matched-account details, user IDs, or security information.

Admin uses **Support requests** to search and filter tickets, open details, move a request through `OPEN`, `IN_PROGRESS`, `RESOLVED`, and `CLOSED`, keep internal notes separate from the user-visible response, and unlock or enable a matched account through the existing secured Admin services. Non-Admin management calls return HTTP 403. Support creation, status changes, resolution/closure, unlock, and enable actions are audited without placing submitted personal text in security-event action names.

PermitWorks never displays or decodes a stored password. A future password-reset feature must use a one-time temporary credential or token, store only a BCrypt hash, require a password change, revoke existing sessions, and audit the reset. No real email or SMS is sent unless an actual notification service is configured; this prototype expects Admin to contact the requester manually.

## Demo accounts and presentation walkthrough

Sign in as the initial Admin and create each account in **User management** by entering the local part shown below and assigning the matching role. Choose unique demo passwords at runtime; passwords are not stored in source.

| Local part / generated email | Role |
|---|---|
| `applicant` / `applicant@permitworks.com` | APPLICANT |
| `department` / `department@permitworks.com` | DEPARTMENT_OFFICER |
| `reviewer` / `reviewer@permitworks.com` | PERMIT_REVIEWER |
| `zoning` / `zoning@permitworks.com` | ZONING_OFFICER |
| `senior` / `senior@permitworks.com` | SENIOR_DEPARTMENT_OFFICER |
| `finance` / `finance@permitworks.com` | FINANCE_OFFICER |
| `inspector` / `inspector@permitworks.com` | BUILDING_INSPECTOR |

Presentation flow:

1. Applicant creates a draft, edits it, and submits it.
2. Department Officer verifies documents and assigns Reviewer and Zoning Officer.
3. Permit Reviewer recommends approval; Zoning Officer recommends approval.
4. Senior Officer requests a correction with remarks.
5. Applicant sees the unresolved correction, edits the application, and resubmits with a response.
6. Reviewer and Zoning Officer approve review round 2.
7. Senior Officer approves the permit.
8. Finance Officer records the fee, a simulated payment reference, and marks it paid; the permit is issued.
9. Department Officer assigns the Building Inspector.
10. Inspector schedules a final inspection, records observations and a pass, then closes the project.
11. Open the permit timeline to show every actor/status change. Admin can monitor it and can separately show security activity.

## Tests and validation

```bat
cd backend
mvn test
mvn verify

cd ..\frontend
npm ci
npm run build
```

Backend tests cover UTF-8 password boundaries and hash stability; both email concepts and login isolation; public support submission/status privacy and cooldown; Admin-only listing, updates, account unlock, and audit events; migration/bootstrap expectations; all workflow role boundaries; ownership; assignments; transitions; correction/resubmission; payment/inspection restrictions; immutable history; lockout; CSRF; token tampering; session expiry/revocation; and logout.

## Safe local demonstration-data reset

[`scripts/reset-local-demo-data.sql`](scripts/reset-local-demo-data.sql) is destructive and must be invoked manually. It aborts unless the connected database is named `permits`, the PostgreSQL server is local loopback, and exactly one enabled, unlocked Admin exists. It runs in one transaction, removes child workflow records before parents, removes every non-Admin account and its sessions, deletes demonstration support requests and related audit events, and verifies that exactly one Admin remains. It never updates the Admin row or `flyway_schema_history`.

First load all Flyway migrations and confirm the configured URL is exactly `jdbc:postgresql://localhost:5432/permits`. Then create a timestamped custom-format backup from PowerShell (enter the password securely when prompted):

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
& 'C:\Program Files\PostgreSQL\17\bin\pg_dump.exe' --host localhost --port 5432 --username permits --dbname permits --format custom --file ("backups\permits-before-demo-reset-{0}.dump" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
```

Verify the backup file is non-empty, then run:

```powershell
& 'C:\Program Files\PostgreSQL\17\bin\psql.exe' --host localhost --port 5432 --username permits --dbname permits --set ON_ERROR_STOP=1 --file scripts\reset-local-demo-data.sql
```

The script prints the counts scheduled for removal. This deletes local demo accounts, sessions belonging to them, permits, assignments, versions, reviews, corrections, payments, inspections, workflow history, and support demonstration data. Afterward, verify Admin login and recreate the Applicant, Department Officer, Permit Reviewer, Zoning Officer, Senior Department Officer, Finance Officer, and Building Inspector accounts through **User management**, supplying a real personal contact email for each.

## Troubleshooting

- **Login rejected:** use the entire lowercase-insensitive `@permitworks.com` address, not the old username.
- **403 on POST/PATCH:** refresh the page so the frontend obtains a fresh CSRF token; confirm the account has the required role.
- **409 conflict:** open the application again. Another action changed its stage, or the requested action is not valid in the current stage.
- **No work on a dashboard:** assignment- and ownership-scoped roles intentionally see only their own records.
- **Database validation failure:** run the application with Flyway enabled and never edit an applied migration. Check `flyway_schema_history` and confirm V4 is present.
- **Cookies not sent:** use the configured exact origin and do not mix `localhost` with `127.0.0.1`.
- **Admin password changed in `.env` but login did not:** bootstrap credentials are used only when `app_users` is empty.

## Remaining prototype limitations

Supporting documents are references/names rather than uploaded binary files. Payment is intentionally simulated. Automated password reset, outbound email/SMS, production TLS, malware scanning, and database-role-level prevention of direct history-table modification remain outside this academic prototype. Admin handles verified support follow-up manually. The application API exposes no update/delete operation for historical reviews, versions, corrections, or workflow history.
