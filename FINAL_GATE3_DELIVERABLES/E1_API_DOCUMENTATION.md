# E1 registration API

Owner Akhil Teja. Additional guide-requested feature for Sprint 2 Review 4 Gate 3. Public registration uses personal email; existing login still requires the assigned @permitworks.com identity.

| Method and path | Access and request | Result |
|---|---|---|
| GET /api/registrations/roles | Public | Existing eight roles only |
| POST /api/registrations | Public + CSRF; fullName, personalEmail, phone, requestedRole, optional description | 201; message, trackingCode, independent activationSecret shown once |
| POST /api/registrations/status | Public + CSRF; personalEmail, trackingCode | 200; state, message, approvedRole, loginEmail after approval, rejectionReason if rejected, activationExpiresAt |
| POST /api/registrations/activate | Public + CSRF; personalEmail, trackingCode, activationSecret, newPassword, confirmPassword | 200; assigned loginEmail; existing sign-in required |
| GET /api/admin/registrations?status=PENDING&page=0 | Admin cookie | 20-row pages; all states when status omitted; no credential hashes |
| POST /api/admin/registrations/{id}/approve | Admin + CSRF; approvedRole, confirm:true | 200; disabled pending-activation user and reviewed request |
| POST /api/admin/registrations/{id}/reject | Admin + CSRF; reason, confirm:true | 200; rejection reason; no user created |

Fetch /api/auth/csrf before public mutations; send X-XSRF-TOKEN from the XSRF-TOKEN cookie. Admin operations use the existing PERMIT_TOKEN HttpOnly cookie. Registration does not issue a session. Existing JWT, lockout, CSRF and session limits remain in force.

Errors: 400 validation/confirmation; 401 missing Admin session or unusable account login; 403 non-Admin, missing CSRF or unavailable/wrong/expired activation; 404 identical unknown email/tracking response; 409 duplicate identity or reviewed request; 429 rate limit. Duplicate eligibility returns the same message for an existing account and an active request, but does reveal that the address is unavailable. Status requires both personal email and unpredictable tracking code.

Input: name <=100 letters/marks/spaces/apostrophes/periods/hyphens; normalized case-insensitive email <=254; phone 7–15 digits, optional leading plus; description <=2000; rejection reason <=500; passwords 8–72 UTF-8 bytes, matching confirmation. Role values are ADMIN, APPLICANT, DEPARTMENT_OFFICER, PERMIT_REVIEWER, ZONING_OFFICER, SENIOR_DEPARTMENT_OFFICER, FINANCE_OFFICER, BUILDING_INSPECTOR.

Per source address, shaped valid requests are limited to 5 submissions/hour, 30 lookups/10 minutes and 10 activation attempts/15 minutes. Failed lookups consume their allowance in a separate transaction. Validation failures precede this limiter. IP windows are database-backed and do not trust arbitrary forwarding headers. Shared NAT users share the limit.

Tracking uses 24 cryptographically random bytes (192 bits) with PWREG- prefix. The independent activation secret uses 32 random bytes (256 bits). Only SHA-256 hashes persist; neither credential appears in Admin DTOs. Activation is single use and expires exactly seven days after approval. BCrypt uses the existing encoder. No SMTP, OTP, SMS or external API is introduced.
