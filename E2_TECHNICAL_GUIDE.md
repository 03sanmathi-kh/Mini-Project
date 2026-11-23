# Annapurna Boratti E2 technical demonstration

Responsibility and implementation: PermitService, PermitController, NotificationService, LocalSmsProvider and WorkflowEntities.

Explain guarded state transitions, ownership/assignment checks, review-round preservation and real-change resubmission. Show normal full flow, an invalid transition, and Could Not Complete followed by rescheduling. E2 advances status after E4 assignment; SMS is a mock and in-app persistence still succeeds if the provider fails.

Dependencies: E1 supplies authenticated identity and permissions; E2 owns status changes; E3 owns transactional records; E4 owns officer allocation. Controllers must not trust browser-supplied actor or owner IDs. Use the source method names in FINAL_TEST_RESULTS.md to show a normal, boundary and failure case, then the original defect and retest evidence.

Five-minute demo: 0-1 responsibility and classes; 1-2 normal input/output; 2-3 invalid or unauthorized case; 3-4 data/history and corrective test; 4-5 dependencies and current limitations. Run commands are in REVIEW_DEMO_GUIDE.md.

Viva prompts: What invariant does your engine enforce? Which engine owns the next operation? What happens if persistence fails? How do two concurrent requests behave? Which claims are tested, mocked, or still unverified? Answer using the actual implementation and the linked test outcomes, rather than claiming every NFR is complete.


## Viva questions and answers

**Who controls permit status?**

PermitService controls status and logs each mutation transactionally. E4 selects and persists assignees; E2 moves the permit to the next review stage after allocation. Applicants cannot submit arbitrary status or actor IDs.

**How are premature decisions blocked?**

decide accepts only PENDING_FINAL_DECISION, reached through guarded technical and zoning review actions. Wrong roles fail authorization; invalid stage changes return 409 without writing false history.

**What makes resubmission valid?**

An open correction is required and content must differ from the previous submission snapshot, or a later document file version must exist. A response string alone does not satisfy the rule. The review round increments while previous reviews and snapshots remain.

**What happens when inspection cannot finish?**

COULD_NOT_COMPLETE requires observations. The result is retained and the permit returns to an issued/reschedulable state. It cannot close until a successful final inspection exists and the current stage is inspection passed.

**What is real in notifications and payment?**

In-app notifications are persisted and filtered to the recipient; read state is enforced by owner ID. SMS is a local provider mock with failure fallback, never a delivery claim. Payment records are explicitly simulated, not real bank transactions.
