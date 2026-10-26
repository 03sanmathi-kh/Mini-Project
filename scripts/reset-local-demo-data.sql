-- DESTRUCTIVE: local-development-only PermitWorks demonstration-data reset.
-- Create and verify a PostgreSQL backup before invoking this file manually.
-- This script deliberately never changes flyway_schema_history or the ADMIN row.

BEGIN;

DO $$
DECLARE
    admin_count integer;
    usable_admin_count integer;
BEGIN
    IF current_database() <> 'permits' THEN
        RAISE EXCEPTION 'Safety check failed: expected database permits, connected to %', current_database();
    END IF;

    IF inet_server_addr() IS NULL OR NOT (
        inet_server_addr() <<= '127.0.0.0/8'::inet OR inet_server_addr() = '::1'::inet
    ) THEN
        RAISE EXCEPTION 'Safety check failed: database server % is not local loopback', inet_server_addr();
    END IF;

    SELECT count(*) INTO admin_count FROM app_users WHERE role = 'ADMIN';
    SELECT count(*) INTO usable_admin_count
      FROM app_users WHERE role = 'ADMIN' AND enabled AND NOT locked;

    IF admin_count <> 1 OR usable_admin_count <> 1 THEN
        RAISE EXCEPTION
          'Safety check failed: expected exactly one enabled, unlocked ADMIN (admins %, usable %)',
          admin_count, usable_admin_count;
    END IF;
END $$;

-- Display the exact records in scope before deletion. psql prints these totals.
SELECT 'non_admin_users' AS record_type, count(*) AS records_to_remove
  FROM app_users WHERE role <> 'ADMIN'
UNION ALL SELECT 'non_admin_sessions', count(*) FROM auth_sessions s
  JOIN app_users u ON u.id = s.user_id WHERE u.role <> 'ADMIN'
UNION ALL SELECT 'permit_applications', count(*) FROM permit_applications
UNION ALL SELECT 'application_versions', count(*) FROM application_versions
UNION ALL SELECT 'application_assignments', count(*) FROM application_assignments
UNION ALL SELECT 'application_reviews', count(*) FROM application_reviews
UNION ALL SELECT 'correction_requests', count(*) FROM correction_requests
UNION ALL SELECT 'payment_records', count(*) FROM payment_records
UNION ALL SELECT 'inspections', count(*) FROM inspections
UNION ALL SELECT 'workflow_history', count(*) FROM workflow_history
UNION ALL SELECT 'support_requests', count(*) FROM support_requests
UNION ALL SELECT 'related_security_events', count(*) FROM security_events e
  WHERE e.action LIKE 'SUPPORT\_%' ESCAPE '\'
     OR EXISTS (SELECT 1 FROM app_users u WHERE u.id = e.actor_id AND u.role <> 'ADMIN')
     OR EXISTS (SELECT 1 FROM app_users u WHERE u.id = e.target_id AND u.role <> 'ADMIN');

-- Application children must be removed before their permit and user parents.
DELETE FROM inspections;
DELETE FROM payment_records;
DELETE FROM correction_requests;
DELETE FROM application_reviews;
DELETE FROM application_assignments;
DELETE FROM application_versions;
DELETE FROM workflow_history;
DELETE FROM permit_applications;

-- Support requests are demonstration data for this one-time reset.
DELETE FROM support_requests;

-- Remove events that would reference deleted users and support-demo audit events.
DELETE FROM security_events e
 WHERE e.action LIKE 'SUPPORT\_%' ESCAPE '\'
    OR EXISTS (SELECT 1 FROM app_users u WHERE u.id = e.actor_id AND u.role <> 'ADMIN')
    OR EXISTS (SELECT 1 FROM app_users u WHERE u.id = e.target_id AND u.role <> 'ADMIN');

DELETE FROM auth_sessions s
 WHERE EXISTS (SELECT 1 FROM app_users u WHERE u.id = s.user_id AND u.role <> 'ADMIN');

DELETE FROM app_users WHERE role <> 'ADMIN';

DO $$
BEGIN
    IF (SELECT count(*) FROM app_users) <> 1
       OR (SELECT count(*) FROM app_users WHERE role = 'ADMIN') <> 1 THEN
        RAISE EXCEPTION 'Post-reset safety check failed: exactly one ADMIN did not remain';
    END IF;

    IF EXISTS (SELECT 1 FROM permit_applications)
       OR EXISTS (SELECT 1 FROM application_versions)
       OR EXISTS (SELECT 1 FROM application_assignments)
       OR EXISTS (SELECT 1 FROM application_reviews)
       OR EXISTS (SELECT 1 FROM correction_requests)
       OR EXISTS (SELECT 1 FROM payment_records)
       OR EXISTS (SELECT 1 FROM inspections)
       OR EXISTS (SELECT 1 FROM workflow_history)
       OR EXISTS (SELECT 1 FROM support_requests) THEN
        RAISE EXCEPTION 'Post-reset safety check failed: demonstration workflow data remains';
    END IF;
END $$;

SELECT count(*) AS remaining_users,
       count(*) FILTER (WHERE role = 'ADMIN') AS remaining_admins,
       bool_and(enabled) AS admin_enabled,
       bool_and(NOT locked) AS admin_unlocked
  FROM app_users;

COMMIT;
