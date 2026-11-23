-- Review remarks are valid on approved requests as well as rejected requests.
-- Retain lifecycle/foreign-key requirements and every existing request.
ALTER TABLE registration_requests DROP CONSTRAINT registration_state_v11;
ALTER TABLE registration_requests ADD CONSTRAINT registration_state_v19 CHECK (
 (registration_status='PENDING' AND active_email IS NOT NULL AND user_id IS NULL AND approved_role IS NULL AND reviewed_at IS NULL AND reviewed_by IS NULL AND admin_remarks IS NULL)
 OR (registration_status='APPROVED' AND active_email IS NOT NULL AND user_id IS NOT NULL AND approved_role IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL)
 OR (registration_status='REJECTED' AND active_email IS NULL AND user_id IS NULL AND approved_role IS NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND admin_remarks IS NOT NULL)
 OR (registration_status='ACTIVATED' AND active_email IS NOT NULL AND user_id IS NOT NULL AND approved_role IS NOT NULL AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND activated_at IS NOT NULL)
);
