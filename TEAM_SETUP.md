# PermitWorks team setup

The shared repository includes all four engines, the E1 registration addition, Flyway migrations through V10, tests, and Gate 3 review documents. Each teammate runs their own local services and database. Local database records, passwords, private activation credentials and backups are intentionally excluded.

## Get the project

```powershell
git clone https://github.com/03sanmathi-kh/Mini-Project.git
Set-Location Mini-Project
```

If you already have a clone, commit or stash your own edits before updating with `git pull --ff-only origin main`. Do not replace your `.env` with somebody else's credentials.

## Run with Docker Desktop

Start Docker Desktop with Linux containers, then run `start-windows.bat`. The setup generates a private, ignored `.env` with random initial credentials. Open http://localhost:5173. Sign in as `admin@permitworks.com` using your own `ADMIN_PASSWORD` from `.env`. Share initial Admin access privately only if a teammate needs it.

Stop with `docker compose down`. Keep the database volume; do not use `down -v` to update the code. Flyway applies pending migrations automatically. For an existing database, back it up before upgrading and retain its original secrets and migration history.

## Native development

Follow the native/IntelliJ instructions in README.md. Use JDK 17+, Maven, Node 22.12+ and PostgreSQL. Configure DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, ADMIN_LOCAL_PART, ADMIN_PASSWORD and ALLOWED_ORIGIN in your own environment. Keep credentials out of tracked IDE configuration. Start the backend with `mvn spring-boot:run` from backend/, and the frontend with `npm ci` followed by `npm run dev` from frontend/. The default frontend is 5173 and backend 8080; ALLOWED_ORIGIN must match the frontend origin.

The development ports 5174 and 5175 in captured review evidence belong to the author's local verification environment. Do not copy its machine-specific process IDs, absolute paths or demo access file. Evidence helper scripts that render Office files or automate Chrome use local Windows runtime paths; adapt those paths before reproducing them.

## Registration demonstration

Select Register, submit personal details and save both independent credentials privately. Check PENDING status using personal email and tracking code. Admin opens Registration Requests, chooses the final approved role and confirms approval or rejection. Approval assigns a PermitWorks login identity; activate with the separate private secret and a new password, then use that assigned identity to sign in. Requested roles do not grant privileges. Help and Support remains inside Register. No SMTP, OTP or SMS service is needed. Lost secrets and expired approvals require manual support; automated reissue is not implemented.

## Tests and review files

Run `mvn verify` in backend/ and `npm ci` then `npm run build` in frontend/. The default backend tests use isolated H2 fixtures. The registration test class also accepts TEST_DB_URL, TEST_DB_DRIVER, TEST_DB_USER and TEST_DB_PASSWORD for a separately created PostgreSQL test database. Never point integration tests at an operational database.

Current recorded validation: 76 passed and one PostgreSQL-only skip in the 77-case H2 suite; 31 registration tests passed on PostgreSQL; frontend build and real Chrome workflow passed. These are captured results, not a claim that tests automatically ran after every clone.

The latest handbook, PPTX, API, test, migration and demonstration records are in [FINAL_GATE3_DELIVERABLES](FINAL_GATE3_DELIVERABLES/FINAL_IMPLEMENTATION_REPORT.md). Guide approval remains pending. See VALIDATION.md for result details and limitations.
