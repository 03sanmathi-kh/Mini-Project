# PermitWorks defects and retests

Original runtime failures are retained in review_evidence/defects or integration logs. Historical handbook failures are explicitly separated from reproduced failures. No failed test was removed to obtain a green run. Fixes are uncommitted working-tree changes.

| ID | Failure | Root cause | Correction | Original evidence | Retest |
|---|---|---|---|---|---|
| D-00 | Application could not compile | Stray trailing 0 after PermitApplication | Removed invalid token | baseline-backend.log | Compile and full verify passed |
| D-01 | Handbook reports expired JWT produced 401 instead of 403 | Token issuance used test Clock; decoder used wall clock | JWT timestamp validator now uses the same injected Clock | Source finding; earlier 401 log was not supplied | All five support tests pass, including Applicant 403 |
| D-02 | Short wrong password used request-length rejection | Password creation policy applied at login | Login keeps bounded nonblank input, credential failures return 401 and count toward lockout | Source/handbook finding; prior runtime sample not retained locally | Dedicated short-password test passes; creation bounds unchanged |
| D-03 | Stale test and migration documentation | README/VALIDATION reflected older runs | Generated counts from actual XML; document V1-V9 and database distinction | Prior VALIDATION.md and report | Updated validation and test register |
| D-04 | Idle timeout removed from code | Absolute-only sessions diverged from approved design | Additive V6 restores last_activity; explicit mutation activity; GET polling does not refresh | Preserved initial patch and source audit | Boundary/activity/absolute tests pass |
| D-05 | JPA context failed: missing application_versions.zone | Snapshot entity gained zone without migration column | Added snapshot zone to new V7 before live migration | iteration-2.log | Schema validation and engine tests pass |
| D-06 | 11 security assertions failed on JWT expiry precision | JWT NumericDate uses seconds, test expected fractions | Use a fixed whole-second controlled test instant | iteration-3.log | Full security regression passes |
| D-07 | Live mutation returned 403 after CSRF cookie rotation | Frontend cached only the initial CSRF token | Read current XSRF cookie for each mutation; fetch when missing | review_evidence/defects/live-csrf-before-fix.log | Live API activity/logout and browser smoke retested |
| D-08 | Four historical completed review assignments counted as unfinished | New completion flag default did not reflect existing current-round reviews | Additive V9 backfills from review-round evidence and terminal state | review_evidence/database/legacy-workload-before.txt | TC_E3_09; live post-migration workload evidence |

Retest commands: `mvn -B -Dpermit.build.directory=target-review verify`; `python scripts/local_review.py isolated-tests`; `python -m unittest discover -s scripts -p test_stage_reference.py -v`. Test-level execution times and outcomes are in FINAL_TEST_RESULTS.md. Local target cleanup stalled, so the configurable target-review output directory avoids stale generated classes without changing application behavior.
