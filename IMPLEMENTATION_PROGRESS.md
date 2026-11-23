# PermitWorks implementation progress

Started 23 November 2026. User authorized implementation of the pasted execution request.

## Baseline

- Preserved pre-existing changes in review_evidence/git/preexisting-changes.patch (local evidence; do not publish without review).
- HEAD 41d92ea95460053f77d2046e3c51be6a100a716f, main; 13 tracked modifications and 2 untracked paths before work.
- Trusted IntelliJ configuration connects to local permits database as postgres. PostgreSQL 17.11; V1–V5 applied; 8 users, 2 permits. No data changed during baseline verification.
- First `mvn -B test` failed compilation: stray `0` after PermitApplication class. Original failure saved in review_evidence/defects/baseline-backend.log. Removed this invalid trailing token.

## Completed implementation milestones

- E4: explicit role/type/zone/stage profiles; PriorityQueue least-load and ID tie break; database mutex for concurrent allocation; queue/alerts; confirmed overrides; workload, processing hours and configurable deadline flags.
- E1: restored idle and absolute limits; successful mutation and explicit activity tracking; GET polling does not refresh; shared Clock for JWT validation; short wrong passwords return 401 and count toward lockout; locked accounts return 423.
- E2: persistent recipient-filtered notifications/read status; local SMS mock/failure fallback; incomplete inspection and rescheduling; unchanged-resubmission guard; E4-backed assignment with E2-owned transitions.
- E3: immutable document bytes/metadata versions, safe download authorization, audit records and PostgreSQL append-only triggers; isolated concurrency, FK and rollback tests.
- Reference module: isolated SQLite staging and read-only queries, explicit mappings, invalid-record reporting; original ZIP unchanged; no credentials or document binaries imported.
- React: routing/workload/queue, notification inbox, reference analytics, audit view, document versions, automatic assignment and confirmed override.

## Verified milestones

- H2 regression: 44 tests, zero failures/errors, one PostgreSQL-specific skip. review_evidence/integration/backend-regression.log.
- PostgreSQL 17.11 isolated engine run: 18 tests passed, zero failures/errors/skips. New isolated database retained; see review_evidence/database/isolated-database.txt.
- PostgreSQL migrations V1–V8 applied in isolation, zero orphan assignments, audit trigger test passed.
- Native Node/Vite production build passed (91 modules). Staging validator tests: 3 passed.
- Failed iterations retained: stray source token, missing snapshot-zone column, test timestamp precision. Fixes retested.
- Live pre-migration custom-format backup created in ignored backups/.

## Final verified state

- Final V9 `mvn verify` passed: 46 cases, 45 passed, zero failures/errors, one PostgreSQL-specific skip on H2. Jar packaged successfully.
- Separate PostgreSQL run: 20/20 passed, including V9 current-round legacy backfill and reopening regression.
- Live PostgreSQL V1-V9 successful. Original eight users/two permits, five assignment rows and four review rows retained. Four completed historical review assignments are now correctly flagged complete; unfinished workload no longer includes them.
- Live API login/read/activity/logout-and-old-token reuse checks passed. Chrome browser smoke passed sign-in, workload, reference analytics, audit, notification views and sign-out; screenshots retained with privacy masking.
- Frontend final production build passed (91 modules); all three staging tests passed. CSV staging imported 32,836 rows and quarantined 3,164; no operational/credential import.
- Updated official-format handbook copy rendered with native Word and page PNGs inspected. Final 21-slide presentation rendered, inspected, exported and passed package/layout/font/import validation. Originals in Downloads preserved.
- Final reports, traceability, defects/retests, Git status, four individual guides with viva answers and checksums generated. New source remains uncommitted; no remote push.
- Backend PID/port in review_evidence/integration/backend-process.json (8080). Frontend PID/port in frontend-process.json (5174). Original 5173 project preserved. Only the requested demonstration services are intentionally left running.

## Remaining external or unverified requirements

Guide approval of documents/design evolution/routing/SLA values; individual rehearsal and AI Mentor; Drive upload; real SMS delivery; 50-user load and human usability targets; forced-crash recovery/ten-second target. These are explicitly not claimed complete. No further code changes are pending for the implemented core scope.

## Constraints

- Preserve original migrations V1–V5, including applied V5. Restore last_activity through additive V6.
- No resets, dropped databases, rewritten history, remote pushes, real SMS, or invented evidence.
- Routing profiles must explicitly specify eligible type/zone/stage. Wildcards are explicit choices.
- No deadline is invented: overdue calculation uses configured SLA hours; missing SLA is reported as unconfigured.
- Guide approval, AI Mentor, and upload remain external actions.
