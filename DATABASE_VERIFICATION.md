# PermitWorks database verification

Baseline live database: PostgreSQL 17.11, localhost:5432/permits, trusted existing IntelliJ configuration. The existing runtime uses the postgres superuser; it should use a dedicated least-privilege runtime role before deployment. Credentials were not printed. Baseline: eight users, two permits, Flyway V1-V5 successful.

Backup: ignored backups/permits-before-sprint2-20261123-200131.dump, 52,284 bytes. No reset/drop/direct operational CSV import was performed.

V5 was already applied and is preserved unchanged. V6 backfills null last_activity from created_at and restores NOT NULL. Existing dormant sessions can require fresh login. V7 adds routing, notification, audit/document records and snapshot zone. V8 installs PostgreSQL append-only UPDATE/DELETE/TRUNCATE guards for audit_log, workflow_history and document_versions. V9 derives finished assignment flags from existing current-round reviews or terminal permits, preserving reopened-round work and all historical records. H2 deliberately skips PostgreSQL-specific trigger syntax.

Isolated PostgreSQL result: 20 tests, 20 pass, 0 fail, 0 skip. Migration history V1-V9 and zero orphan assignments are recorded in review_evidence/database/isolated-integrity.txt. Test database names are retained in isolated-database.txt; no original records were used as failure-injection fixtures.

Live verification output: review_evidence/database/live-schema.txt.

Reference data is staged separately in ignored reference_data/reference.sqlite. Password hashes, personal names/emails/addresses and document paths are excluded. Status mappings retain ambiguous REFERENCE categories; Inspection Officer is quarantined. Original ZIP SHA-256: d0547b53196163ff9039ef9fc1fee05df7183a3eb2772c130f0e33dc84cd996f. Provenance remains unverified. See reference_data/summary.json and invalid_records.json for counts and reasons.

Recovery scope: rollback is measured in an isolated transaction. No forced crash, power-loss durability or ten-second crash recovery result is claimed. Database superusers can disable triggers; these protections are not proof against a malicious superuser.


Final live check: PostgreSQL V1-V9 applied successfully, eight original users and two original permits preserved. Live API login/read/activity/logout checks passed, including reuse rejection of the revoked cookie. Browser smoke passed login, workload, reference analytics, audit and notifications with zero page errors. Screenshots hide contact identity and blur officer names. Evidence: review_evidence/database/live-schema.txt; review_evidence/integration/live-api-smoke.json and browser-smoke.json.
