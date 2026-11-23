# Registration traceability addition

The following E1-REG identifiers are local labels for this additional guide-requested feature, not newly invented approved FR numbers. Existing FR-01–11 and NFR-01–08 mappings remain in the project REQUIREMENTS_TRACEABILITY_MATRIX.md.

| Local feature | Source and API | Evidence |
|---|---|---|
| E1-REG-01 Single entry and validated request | Login; Registration.jsx; POST /registrations | TC_REG_01–07; browser button and modal |
| E1-REG-02 Private status | status endpoint; hashed random tracking | TC_REG_08,24,25; Pending/Approved/Rejected screenshots |
| E1-REG-03 Admin decision and role | AdminRegistrationRequests; approve/reject | TC_REG_09–14,23,32; browser requested ADMIN approved APPLICANT |
| E1-REG-04 Private activation and existing login | activate; AuthService | TC_REG_15–20,27–29,31; successful browser login and Admin 403 |
| E1-REG-05 Additive data and concurrency | V10; IdentityGuard; mutex and row locks | TC_REG_22,23,30,31; original-record signatures |
| E1-REG-06 Existing support and build | secondary Help; original support controllers | TC_REG_21; TC-REG-26; support screenshot |

NFR boundaries: automated concurrency is not 50-user performance certification. Browser verification is not a ten-person usability trial. SMTP/OTP/SMS and external delivery are outside this registration implementation. Human guide approval is pending.
