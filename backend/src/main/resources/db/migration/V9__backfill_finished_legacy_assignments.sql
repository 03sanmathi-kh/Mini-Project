-- Derive the new workload flag from existing evidence, without replacing any history.
UPDATE application_assignments SET completed = TRUE
WHERE active = TRUE AND (
 EXISTS (
  SELECT 1 FROM permit_applications p
  WHERE p.id = application_assignments.application_id
    AND p.status IN ('PROJECT_CLOSED','REJECTED')
 ) OR EXISTS (
  SELECT 1 FROM application_reviews r
  JOIN permit_applications p ON p.id = r.application_id
  WHERE r.application_id = application_assignments.application_id
    AND r.review_round = p.review_round
    AND ((application_assignments.assignment_type = 'PERMIT_REVIEWER' AND r.review_type = 'TECHNICAL')
      OR (application_assignments.assignment_type = 'ZONING_OFFICER' AND r.review_type = 'ZONING'))
 )
);
