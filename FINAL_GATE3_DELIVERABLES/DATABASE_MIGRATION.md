# Registration database migration

V10__registration_approval_activation.sql follows the actual V9 migration. Do not edit already-applied V1–V9. Flyway applies V10 during normal backend startup; Hibernate validates the schema afterward.

app_users gains activation_pending BOOLEAN NOT NULL DEFAULT FALSE. All existing users retain passwords, roles and enablement. registration_requests stores normalized contact fields, requested/approved existing role names, state, hashed credentials, reviewer/time, rejection reason, linked account, activation deadline and consumption time. CHECK constraints enforce consistent PENDING, APPROVED, REJECTED and ACTIVATED records. Unique active_email reserves an identity while active; rejection releases it. user_id and tracking_hash are unique; user/reviewer foreign keys reference app_users. Status/time and email indexes support review and lookup.

registration_mutex serializes duplicate identity checks, approval and activation with pessimistic locks. registration_limits persists hashed source identifiers, windows and attempts. The global mutex favors correctness for this small project; it is not a throughput benchmark.

A private pre-migration PostgreSQL custom-format backup is retained under backups/. No reset, DROP, record deletion or down migration was performed. Browser and PostgreSQL test databases were newly created and retained separately. evidence/record-preservation.json compares original IDs and record signatures; login counter/timestamp mutations are excluded from user identity signatures.

Recovery: stop the application, investigate and restore the private backup to a separately named recovery database if needed. Do not run the old reset script or overwrite the operational database as a routine rollback.
