# PermitWorks registration progress

User authorized complete guide-requested E1 registration, Admin review, private activation, tests and updated review documents. Owner: Akhil Teja.

Baseline: existing working changes preserved in ignored FINAL_GATE3_DELIVERABLES/private-build/pre-registration.patch; main/HEAD 41d92ea unchanged. Live PostgreSQL 17.11 has V1-V9, eight users and two permits. A fresh private backup was taken before the additive registration migration. No registration module exists.

Design: personal email is used for requests/status; approval generates a unique PermitWorks login address. Requested roles are existing enums only and do not grant access. Admin approval creates a disabled pending-activation account. Independent random tracking and activation credentials are generated; only hashes are stored. Activation is single-use, expires seven days after approval and uses the existing BCrypt/password/JWT system. Support remains available through a secondary Help link.

In progress: V10 schema, guarded service/endpoints, registration/Admin UI, tests. All production records and already-applied migrations are preserved. No SMTP, OTP, external provider or remote push.

Verification: V10 implemented and applied to live PostgreSQL. Full Maven verify 77 executed,76 passed,1 PostgreSQL-only H2 skip; PostgreSQL registration31/31; frontend92 modules production build passed. All seven original-record signature groups PASS; live8users2permits0registrationfixtures. Browser approval/private activation flows undergoing final script timing fixes. Review records builder and updated deck in progress. Original HEAD/no-push unchanged.

Browser verification PASS: full approval/activation/login/logout plus separate rejection; button dimensions, support, Escape focus and mobile verified. Live existing Admin login and registration list200, core API smoke and logout revocation passed. Isolated demo services stopped; database retained. Gate3 registers and handbook generated;28-slide PPTX validated with zero package/layout findings. Handbook visual QA pending.

Handbook QA complete:19 native Word rendered pages inspected; no clipped rows or blank-page errors. Updated deck typography visually checked, final28-slide receipt being saved under a fresh filename after the finalizer correctly refused receipt overwrite. Root E1 guide, RTM and historical reports now point to current registration evidence.

Completed: registration implementation, V10 migration, full backend regression, PostgreSQL registration tests, frontend production build, isolated real-Chrome flow, live Admin/API smoke, original-data preservation, handbook and PPTX, current E1 registers and documentation. Final handbook19 pages visually inspected; final presentation28 slides with0 integrity/layout findings and successful first-party import. Tracking/private secrets redacted in diagnostic evidence and blurred in screenshots. Demo services stopped; updated main services remain8080/5174. No original data removed, no remote push, no fabricated guide approval. Manual lost-secret/expired-approval recovery remains a documented limitation.

Publication authorized by user: implementationf126f0b and remote README merge532e506 pushed to shared main on23November2026. Remote main and backup feature branch verified at532e506. Credentials/private files excluded; TEAM_SETUP.md added. Earlier no-push statements record the pre-publication stage.
