The latest additional E1 registration implementation and evidence are in [the registration final report](FINAL_GATE3_DELIVERABLES/FINAL_IMPLEMENTATION_REPORT.md). The four-engine report below is preserved as earlier project context.

# PermitWorks implementation report

Implemented 23 November 2026 in the existing repository. Engine ownership and original Git history are preserved. No remote push or local commit was created; mixed pre-existing changes remain available for review.

E1 now enforces 30-minute inactivity and eight-hour absolute expiry with a consistent testable clock, 401 credential failures and 423 lockout. E4 now owns eligibility and least-loaded selection, queue/alert, confirmed replacement, optional SLA deadlines, filtered workload and processing statistics. E2 calls E4, keeps workflow ownership, persists notifications, rejects unchanged resubmission and handles incomplete inspections without closing. E3 adds immutable document versions and audit records, with tested atomicity, FK and concurrency behavior and PostgreSQL evidence triggers.

React retains the existing design and adds workload/routing, queue/retry, notifications, audit, file versions and CSV analytics. The original archive is staged separately, preserving ambiguous mappings and excluding credentials/personal text. Reference count totals and quarantine reasons are in reference_data/summary.json and invalid_records.json.

H2 regression: 46 executions, 45 passed, 0 failed, 1 PostgreSQL-only skipped. PostgreSQL engine run: 20 executions, 20 passed, 0 failed, 0 skipped.

Additional checks: frontend production build passed; three staging tests passed. Genuine failure/retest logs are retained and sanitized. Final live schema/row-preservation output and browser/API smoke results belong under review_evidence/database and integration; inspect those files before claiming live readiness. Handbook and review presentation are editable copies under review_evidence/documents; original Downloads files are preserved.

Remaining limitations: actual routing/SLA choices and API/schema evolution need guide approval; no real SMS delivery, 50-user load certification, human usability study or forced-crash recovery/10-second certification; file magic/size checks do not replace malware scanning. Document bytes are stored as base64 in the protected database for this prototype. Local runtime currently uses a superuser and should use a dedicated restricted role before deployment. H2 does not exercise PostgreSQL trigger syntax. The old destructive demo-reset script conflicts with append-only protections and must not be used on the migrated database; use an isolated demonstration database instead.

External requirements remain pending: guide approval, each student's AI Mentor activity, designated Drive upload and individual rehearsal. See ENGINE_READINESS.md, DATABASE_VERIFICATION.md, FINAL_TEST_RESULTS.md, DEFECT_RETEST_REPORT.md and REQUIREMENTS_TRACEABILITY_MATRIX.md.


Final live check: PostgreSQL V1-V9 applied successfully, eight original users and two original permits preserved. Live API login/read/activity/logout checks passed, including reuse rejection of the revoked cookie. Browser smoke passed login, workload, reference analytics, audit and notifications with zero page errors. Screenshots hide contact identity and blur officer names. Evidence: review_evidence/database/live-schema.txt; review_evidence/integration/live-api-smoke.json and browser-smoke.json.
