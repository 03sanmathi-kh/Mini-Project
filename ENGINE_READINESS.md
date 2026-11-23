# PermitWorks engine readiness

H2 regression: 46 executions, 45 passed, 0 failed, 1 PostgreSQL-only skipped. PostgreSQL engine run: 20 executions, 20 passed, 0 failed, 0 skipped.

| Engine | Owner | Demonstrable core | Evidence | Remaining limits |
|---|---|---|---|---|
| E1 | Akhil Teja | Login, eight roles, CSRF, lockout, idle/absolute expiry, revocation and Admin management | Security and support integration suites | TLS deployment/security attributes must be validated in deployment |
| E2 | Annapurna Boratti | Lifecycle, correction rounds, notifications, incomplete inspections, E4 assignment integration | Existing full-flow suite and TC_E2_01-03 | SMS mock; workflow/API design evolution requires guide review |
| E3 | Amruta Kutri | Transactions, concurrent writes, FK rejection, document versions and append-only audit | TC_E3_01-09 on PostgreSQL | Crash recovery and malicious-superuser protection not certified |
| E4 | Sanmathi Hegde | Eligible least-load routing, queue, confirmed override, SLA flags, reports and reference analytics | TC_E4_01-08; staging tests | Guide must approve actual profiles/SLA settings; no arbitrary deadlines supplied |

Core technical demonstrations are supported by automated tests. Final presentation/handbook approval, individual understanding, AI Mentor completion and upload cannot be certified by these tests. See FINAL_IMPLEMENTATION_REPORT.md for browser/live verification and final limitations.
