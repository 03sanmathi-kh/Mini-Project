The additional guide-requested registration feature is mapped as E1-REG-01 through 06 in [registration traceability](FINAL_GATE3_DELIVERABLES/REQUIREMENTS_TRACEABILITY_MATRIX.md). These local feature labels supplement the approved FR/NFR mapping below; they do not invent approved requirement numbers.

# PermitWorks requirements traceability

Labels below paraphrase the supplied handbook and Gate 2 deck. The complete original Gate 1 wording was not supplied; no new acceptance criteria are invented. Engine ownership is unchanged. Evidence: FINAL_TEST_RESULTS.md, review_evidence/database/postgresql-engine-tests.log, integration logs and staging test log.

| ID | Requirement | Engine | Code | API | Test | Status | Limitations |
|---|---|---|---|---|---|---|---|
| FR-01 | Login and account access | E1 | AuthService / SecurityConfig / AdminService | /api/auth/*; /api/admin/users | SecurityIntegrationTest | PASS | BCrypt, sessions, CSRF, roles and lockout tested |
| FR-02 | Create and submit applications | E2 | PermitService.create/edit/submit | /api/permits | PermitWorkflowIntegrationTest; TC_E3_06 | PASS | Existing flow retained; creation/API evolution needs guide review |
| FR-03 | Eligible workload-based assignment and override | E4 | AssignmentEngine | /api/permits/{id}/auto-assignment/{type}; /override/{type} | TC_E4_01 through TC_E4_08 | PASS | Explicit profiles and optional SLA; actual routing/SLA choices need approval |
| FR-04 | Correction, resubmission and document versions | E2/E3 | PermitService.resubmit; DocumentController | /api/permits/{id}/resubmit; /documents/files | TC_E2_01; TC_E3_05; existing full flow | PASS | File-type allowlist and 5 MB limit; malware scanning not implemented |
| FR-05 | Valid status transitions | E2 | PermitService | /api/permits/{id} action routes | Existing invalid transition/full-flow tests | PASS | Granular states differ from original diagram; approval pending |
| FR-06 | Review-dependent senior decision | E2 | PermitService.decide | /api/permits/{id}/decision | Existing full-flow/rejection tests | PASS | Implemented HTTP contract differs from original 422 design |
| FR-07 | Fee and simulated payment | E2/E3 | PermitService.payment | /api/permits/{id}/payment | Full flow; TC_E3_04 | PASS | Simulation only; payment changes workflow status as documented |
| FR-08 | Inspection and closure | E2 | PermitService.recordInspection/close | /api/permits/{id}/inspections | Full flow; TC_E2_02 | PASS | Could Not Complete returns to issued/reschedulable state |
| FR-09 | In-app and SMS notifications | E2 | NotificationService; SmsProvider | /api/notifications | TC_E2_03; workflow/queue tests | PARTIAL | In-app verified; SMS mock/fallback verified, no external delivery |
| FR-10 | Identity and action audit history | E3 | AuditService; V8 migration | /api/audit; permit history | TC_E3_01; TC_E3_08 | PASS | PostgreSQL triggers and API restrictions; superuser can disable protections |
| FR-11 | Workload and reference analytics | E4 | AssignmentEngine.report; stage_reference.py | /api/reports/workload; /api/reference/analytics | TC_E4_05/08; staging tests | PASS | Reference provenance unverified; ambiguous records quarantined |
| NFR-01 | 50 users with 95 percent within 3 seconds | All | API and persistence | All | Not executed | NOT TESTED | Concurrency correctness tests are not a 50-user load benchmark |
| NFR-02 | Role-based security | E1 | SecurityConfig; controller/service guards | Protected API | Eight-role and E4 endpoint tests | PASS | No generic Admin bypass for permit review/payment |
| NFR-03 | Passwords, lockout and session security | E1 | AuthSession; AuthService | /api/auth/* | SecurityIntegrationTest | PASS | 29/30/31 minute idle, explicit activity, absolute limit and revocation |
| NFR-04 | 9 of 10 users submit within 5 minutes | All | React forms | Applicant UI | Human protocol only | NOT TESTED | Recruit participants and record genuine outcomes |
| NFR-05 | Recovery within 10 seconds | E3 | Transactions and database recovery | Persistence | TC_E3_07 | PARTIAL | Rollback restoration measured; process crash/durability target unverified |
| NFR-06 | Required audit fields and protection | E3 | audit_log; workflow_history; security_events | /api/audit | TC_E3_08; history assertions | PASS | Actor, action, date/time, entity and details retained |
| NFR-07 | Referential integrity | E3 | Flyway FK constraints | Persistence | TC_E3_02; live schema validation | PASS | PostgreSQL and H2 invalid-reference checks |
| NFR-08 | Notification timing and fallback | E2 | NotificationService | /api/notifications | TC_E2_03 | PARTIAL | Synchronous local persistence verified; external timing not measured |
