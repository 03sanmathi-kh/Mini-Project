ALTER TABLE application_versions ADD COLUMN property_id BIGINT REFERENCES properties(property_id);
ALTER TABLE application_versions ADD COLUMN work_type VARCHAR(80);
ALTER TABLE application_versions ADD COLUMN application_type VARCHAR(80);
UPDATE application_versions SET work_type=permit_type,application_type=permit_type;
