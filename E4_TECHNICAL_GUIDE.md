# Sanmathi Hegde E4 technical demonstration

Responsibility and implementation: AssignmentEngine, EngineController, RoutingProfiles, AssignmentMutexes, Workload React panel and stage_reference.py.

Explain role/type/zone/stage eligibility, PriorityQueue ordering by active work then lowest ID, and database mutex serialization. Show least-loaded assignment, unmatched-zone queue/alert, confirmed override with previous/new officer, configured deadlines and report filters. Explain why CSV reference IDs and hashes are never operational credentials.

Dependencies: E1 supplies authenticated identity and permissions; E2 owns status changes; E3 owns transactional records; E4 owns officer allocation. Controllers must not trust browser-supplied actor or owner IDs. Use the source method names in FINAL_TEST_RESULTS.md to show a normal, boundary and failure case, then the original defect and retest evidence.

Five-minute demo: 0-1 responsibility and classes; 1-2 normal input/output; 2-3 invalid or unauthorized case; 3-4 data/history and corrective test; 4-5 dependencies and current limitations. Run commands are in REVIEW_DEMO_GUIDE.md.

Viva prompts: What invariant does your engine enforce? Which engine owns the next operation? What happens if persistence fails? How do two concurrent requests behave? Which claims are tested, mocked, or still unverified? Answer using the actual implementation and the linked test outcomes, rather than claiming every NFR is complete.


## Viva questions and answers

**How is eligibility established?**

A saved profile matches role, permit type, zone and assignment stage; the account must be enabled and unlocked. The permit state must allow that stage. A wildcard is an explicit configuration choice, not an assumption that every officer covers every zone.

**How does the heap choose?**

Candidates are ordered by unfinished active assignment count, then lowest officer ID. Repeated insertion builds the current heap in O(n log n); removal is O(log n). The database queries used to count workloads also contribute to total cost; this is not a load benchmark.

**What if no officer is eligible?**

No invalid assignment is made. A unique permit/stage queue entry records the reason and a Department Officer in-app alert is persisted. Configure a matching profile and retry; do not invent an officer or silently advance the workflow.

**What permits a replacement?**

Only Admin or Department Officer can manage routing. Replacing an existing assignment requires explicit confirmation and a nonblank reason; the old row is deactivated, previous/new IDs and timestamp are retained and an audit record is appended.

**How are deadlines and CSV analytics interpreted?**

A configured SLA sets a deadline for new allocations. Missing SLA is reported as unconfigured, not overdue. V9 fixes historical workload flags using current-round reviews. CSV analytics use an isolated reference store with ambiguous roles quarantined and no credential or document-file import.
