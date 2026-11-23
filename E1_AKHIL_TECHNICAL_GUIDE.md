Current registration addition: see [E1 individual demonstration guide](FINAL_GATE3_DELIVERABLES/E1_INDIVIDUAL_DEMONSTRATION_GUIDE.md), [API register](FINAL_GATE3_DELIVERABLES/E1_API_DOCUMENTATION.md), and [implementation register](FINAL_GATE3_DELIVERABLES/E1_IMPLEMENTATION_REGISTER.md). Registration is owned by Akhil Teja and preserves the existing E1 mechanisms below. Guide approval remains pending.

# Akhil Teja E1 technical demonstration

Responsibility and implementation: AuthService, AuthSession, SessionFilter, SecurityConfig, AuthController and AdminService.

Explain authentication versus authorization, BCrypt one-way verification, HttpOnly versus CSRF, JWT signature versus server revocation, five failures and 423 lock status. A short wrong password is 401; malformed input remains 400. Polling reads do not extend idle expiry. Show the 29/30/31-minute and eight-hour tests; never wait eight real hours in a demo.

Dependencies: E1 supplies authenticated identity and permissions; E2 owns status changes; E3 owns transactional records; E4 owns officer allocation. Controllers must not trust browser-supplied actor or owner IDs. Use the source method names in FINAL_TEST_RESULTS.md to show a normal, boundary and failure case, then the original defect and retest evidence.

Five-minute demo: 0-1 responsibility and classes; 1-2 normal input/output; 2-3 invalid or unauthorized case; 3-4 data/history and corrective test; 4-5 dependencies and current limitations. Run commands are in REVIEW_DEMO_GUIDE.md.

Viva prompts: What invariant does your engine enforce? Which engine owns the next operation? What happens if persistence fails? How do two concurrent requests behave? Which claims are tested, mocked, or still unverified? Answer using the actual implementation and the linked test outcomes, rather than claiming every NFR is complete.


## Viva questions and answers

**How is a password checked?**

BCryptPasswordEncoder uses cost 12. The app compares a candidate with the stored one-way hash; it never decrypts a password. Creation requires 8-72 UTF-8 bytes, while a bounded nonblank wrong login candidate returns 401 and counts as a failed attempt.

**What do JWT and server sessions each provide?**

The signed JWT carries issuer, user subject, session ID, email and expiry. The server session supplies revocation and idle tracking. The filter reloads the enabled/unlocked user and current role from the database rather than trusting a browser role.

**Why HttpOnly and CSRF together?**

HttpOnly prevents JavaScript from reading PERMIT_TOKEN. Browsers still send that cookie automatically, so mutations require the XSRF cookie/header pair. The XSRF cookie is readable so the frontend can send the current token after rotation; it is not the authentication token.

**What extends a session?**

Successful mutations or POST /api/auth/activity before expiry update lastActivity. Real pointer/key input is throttled to one activity request per minute. GET polling never extends idle time. Exactly 30 idle minutes expires; no activity extends the eight-hour absolute limit.

**How do you demonstrate failures?**

Show missing/tampered token denial, wrong-role 403, short wrong password 401, fifth failure/locked login 423, Admin-only unlock, disabled-user revocation and logout-token reuse rejection. Controlled Clock tests demonstrate time boundaries without waiting real hours.
