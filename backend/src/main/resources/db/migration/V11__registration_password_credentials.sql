-- Two-credential registration: applicant-chosen registration password (BCrypt) for status checks only.
-- Additive: existing rows are kept; legacy rows simply have no registration password hash.
ALTER TABLE registration_requests ADD COLUMN registration_password_hash VARCHAR(100);
ALTER TABLE registration_requests ALTER COLUMN tracking_hash DROP NOT NULL;
ALTER TABLE registration_requests ADD CONSTRAINT registration_password_bcrypt
 CHECK (registration_password_hash IS NULL OR registration_password_hash LIKE '$2%');

-- Replace the tracking/activation state rule with one that does not require activation secrets.
ALTER TABLE registration_requests DROP CONSTRAINT registration_state;
ALTER TABLE registration_requests ADD CONSTRAINT registration_state_v11 CHECK (
 (status='PENDING' AND active_email IS NOT NULL AND user_id IS NULL AND approved_role IS NULL AND reviewed_at IS NULL AND reviewed_by IS NULL AND rejection_reason IS NULL)
 OR (status='APPROVED' AND active_email IS NOT NULL AND user_id IS NOT NULL AND approved_role IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND rejection_reason IS NULL)
 OR (status='REJECTED' AND active_email IS NULL AND user_id IS NULL AND approved_role IS NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND rejection_reason IS NOT NULL)
 OR (status='ACTIVATED' AND active_email IS NOT NULL AND user_id IS NOT NULL AND approved_role IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND activated_at IS NOT NULL AND rejection_reason IS NULL)
);

-- Public registration can never be approved into the Admin role.
ALTER TABLE registration_requests DROP CONSTRAINT registration_approved_role;
ALTER TABLE registration_requests ADD CONSTRAINT registration_approved_role_v11
 CHECK (approved_role IS NULL OR approved_role IN ('APPLICANT','DEPARTMENT_OFFICER','PERMIT_REVIEWER','ZONING_OFFICER','SENIOR_DEPARTMENT_OFFICER','FINANCE_OFFICER','BUILDING_INSPECTOR'));

CREATE INDEX registration_personal_email_submitted ON registration_requests(personal_email, submitted_at);
