# Akhil Teja E1 registration demonstration

Explain that E1 owns access control and communication. Registration is an additional guide-requested feature for Sprint 2 Review 4 Gate 3. Use a newly created isolated demo database; keep the operational eight users and two permits intact.

1. Open http://localhost:5174 for the installed local application. Show the single Register button in the former Get help position. Demonstrate Help and Support inside the registration modal.
2. On an isolated demo, enter a name, personal email, phone and requested role. Submit. Privately save both tracking code and the separate activation secret; never paste them into slides or Git.
3. Check status using personal email plus tracking code: PENDING. Explain that submission creates no login user.
4. Sign in as Admin; open Registration Requests. Review details, choose the approved role and confirm. Requested ADMIN does not grant ADMIN; the example approves APPLICANT. Approval creates a disabled pending-activation account.
5. Sign out. Check APPROVED status and note the assigned @permitworks.com login identity. Set a password with the separately saved secret. The private secret expires seven days after approval and is consumed once.
6. Sign in using the assigned email and new password. Show APPLICANT permissions and logout. Demonstrate a separate rejected request and the verified user-visible reason.
7. Show V10, actual test summaries and screenshots. Explain BCrypt, hashed random credentials, CSRF, existing JWT cookies and serialization preventing duplicate accounts.

Viva boundaries: personal email is not the login identity. Tracking alone cannot activate. Admin cannot bypass activation through Enable. No email/SMS is sent. Lost secrets and expired approvals require manual Help and identity verification; there is no implemented reset/reissue workflow. The demonstration proves observed assertions; guide approval, load certification and human usability acceptance are not claimed.

Reproduce isolated demo after packaging: python scripts/registration_environment.py demo; read credentials privately from ignored private-build/demo-access.json, open http://localhost:5175, then python scripts/registration_environment.py stop-demo. Do not share the private access file. This creates a separate retained database and leaves operational records intact.
