# PermitWorks current registration validation

23 November 2026. Full H2 regression: 77 executed, 76 passed, 0 failed, 1 skipped. PostgreSQL registration: 31 executed, 31 passed, 0 failed, 0 skipped. Frontend production build: PASS, 92 modules. Browser workflow: PASS.

Current full source: backend/target-registration/surefire-reports. Current PostgreSQL registration suite: backend/target-registration-pg/surefire-reports. Frontend build: FINAL_GATE3_DELIVERABLES/evidence/registration-frontend-build-final.log. Browser: FINAL_GATE3_DELIVERABLES/evidence/registration-browser-results.json. Live V1–V10 and original-data preservation: FINAL_GATE3_DELIVERABLES/evidence/live-v10-schema.txt and record-preservation.json.

Earlier 46-case H2 / 20-case PostgreSQL engine checks and CSV staging tests remain historical in review_evidence. They are not counted as additional current V10 executions. See FINAL_GATE3_DELIVERABLES/E1_TEST_CASE_REGISTER.md for every current case. No frontend unit-test command exists; build and actual Chrome checks were run. Broader NFR limits and human approval remain documented.
