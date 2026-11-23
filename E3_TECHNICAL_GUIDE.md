# Amruta Kutri E3 technical demonstration

Responsibility and implementation: WorkflowRepositories, AuditService, DocumentController, Flyway V1-V9 and EngineIntegrationTest.

Explain atomic transactions, pessimistic row locks versus version columns, foreign-key rejection and controlled concurrent operations. Show rollback after multiple writes, old document bytes, audit fields and PostgreSQL trigger rejection. Measured rollback latency is not forced-crash recovery or proof of power-loss durability.

Dependencies: E1 supplies authenticated identity and permissions; E2 owns status changes; E3 owns transactional records; E4 owns officer allocation. Controllers must not trust browser-supplied actor or owner IDs. Use the source method names in FINAL_TEST_RESULTS.md to show a normal, boundary and failure case, then the original defect and retest evidence.

Five-minute demo: 0-1 responsibility and classes; 1-2 normal input/output; 2-3 invalid or unauthorized case; 3-4 data/history and corrective test; 4-5 dependencies and current limitations. Run commands are in REVIEW_DEMO_GUIDE.md.

Viva prompts: What invariant does your engine enforce? Which engine owns the next operation? What happens if persistence fails? How do two concurrent requests behave? Which claims are tested, mocked, or still unverified? Answer using the actual implementation and the linked test outcomes, rather than claiming every NFR is complete.


## Viva questions and answers

**What proves atomicity?**

A spy injects an exception after permit/history/audit writes inside a service transaction. The test checks unchanged counts afterward. Separate FK tests attempt invalid application, applicant and officer references. No operational database record is a failure fixture.

**Why locks and versions?**

Permit mutation acquires PESSIMISTIC_WRITE inside a transaction. The entity also has a Hibernate version column. Allocation additionally locks a database mutex before the permit, serializing least-load choices across concurrent requests and application instances.

**How are audit records protected?**

The API exposes read-only audit/history operations with role and visibility checks. PostgreSQL V8 rejects UPDATE, DELETE and TRUNCATE on audit_log, workflow_history and document_versions. A superuser can disable triggers, so this is not a malicious-superuser guarantee.

**How do real document versions differ from text references?**

Each uploaded version stores immutable bytes as base64 plus application/name/version, content type, size, SHA-256, uploader and time. Owner/status checks protect uploads and permit visibility protects downloads. CSV FilePath strings do not create actual files.

**What does the recovery result establish?**

The measured ROLLBACK_RECOVERY_MS verifies restoration after a deliberate transaction failure. It does not certify process crash, power-loss durability or a ten-second recovery target. V9 separately derives old assignment completion from current-round review evidence and does not rewrite history.
