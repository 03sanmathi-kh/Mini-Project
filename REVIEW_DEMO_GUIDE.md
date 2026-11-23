# PermitWorks local run and review demonstration

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


Final live check: PostgreSQL V1-V9 applied successfully, eight original users and two original permits preserved. Live API login/read/activity/logout checks passed, including reuse rejection of the revoked cookie. Browser smoke passed login, workload, reference analytics, audit and notifications with zero page errors. Screenshots hide contact identity and blur officer names. Evidence: review_evidence/database/live-schema.txt; review_evidence/integration/live-api-smoke.json and browser-smoke.json.

Current running UI: http://localhost:5174 (5173 belongs to another project). Use `$env:ALLOWED_ORIGIN='http://localhost:5174'` before backend start and `--port 5174` for Vite. The normal default remains 5173 when available.
