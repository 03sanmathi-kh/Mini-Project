# PermitWorks integration contract

Authentication uses `{email,password}` and only normalized `@permitworks.com` addresses. Admin creation uses `{fullName,localPart,personalContactEmail,password,role}`. Never trust a browser-supplied role, owner ID, actor ID, status, or current assignee.

The complete endpoint inventory and role matrix are in [README.md](README.md). All workflow routes live below `/api/permits`; all mutations require CSRF and run through `PermitService` transactions. `GET /api/permits/dashboard` and `GET /api/permits/{id}` apply owner/active-assignment/stage visibility before returning data.

Integration rules:

- Use the authenticated `currentUser` established by `SessionFilter`.
- Return 423 for locked login, 401 for invalid credentials/session, 403 for a forbidden role or valid-but-unassigned actor, 404 when a record is absent or intentionally hidden from that actor, 409 for a state-machine conflict, and 400 for invalid input.
- Do not add generic Admin bypasses to technical review, zoning, senior decision, payment, or inspection operations.
- Reassignment requires explicit confirmation and a reason, deactivates the prior assignment and preserves previous/new officer IDs. E4 owns selection/persistence; E2 advances workflow state. Completed review ownership remains visible but is excluded from unfinished workload. Reviews, submission snapshots, corrections, inspections, and workflow history are append-only through the API.
- `security_events` remains the login/account-security stream. `workflow_history` is the separate permit action stream and stores an actor identity snapshot.
- Any future file-upload implementation must authorize against the database-owned application before reading or writing content.

Required regression checks after changes: cross-applicant access, cross-assignee review/inspection, invalid transition conflicts, disabled/role-changed session revocation, CSRF enforcement, correction-round history, payment-before-approval denial, and closure-before-final-pass denial.
