# E1 registration implementation register

Owner Akhil Teja. Additional guide-requested feature; human guide approval is pending.

| Component | Implementation and checked behavior |
|---|---|
| Login entry | Original Get help position and 82.453125 by 38 px footprint now contain one Register button |
| Public forms | Registration.jsx: validated details, eight requested roles, confirmation and status, private password setup, accessible modal and secondary support |
| Identity | RegistrationIdentityGuard: shared transaction mutex across public requests and direct Admin user creation/update; normalized account and request checks |
| Review | Admin-only paginated list; explicit confirmation; least-privilege default APPLICANT; selected role becomes actual user role |
| Persistence | V10 additive tables and app_users.activation_pending; previous migration checksums retained |
| Activation | Independent private secret, exact seven-day deadline, password confirmation/BCrypt, consumed secret, locked transaction |
| Bypass prevention | AuthService rejects activation_pending even if enabled was changed; Admin enable refuses pending accounts |
| Evidence | Audit actor/action/time/entity for review and activation; public submission creates a security event without exposing personal details |
| Existing flows | Existing support routes and Admin support, JWT cookies, CSRF, lockout, idle/absolute session handling retained |
