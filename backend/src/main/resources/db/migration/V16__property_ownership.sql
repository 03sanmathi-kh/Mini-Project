ALTER TABLE properties ADD COLUMN owner_id BIGINT REFERENCES app_users(id);
UPDATE properties p SET owner_id=(SELECT a.applicant_id FROM applications a WHERE a.property_id=p.property_id ORDER BY a.application_id LIMIT 1);
CREATE INDEX idx_properties_owner ON properties(owner_id);
ALTER TABLE applicants ADD CONSTRAINT uq_applicants_email UNIQUE(email);
ALTER TABLE officers ADD CONSTRAINT uq_officers_email UNIQUE(email);
ALTER TABLE payments ADD CONSTRAINT ck_payment_status CHECK(payment_status IN ('PENDING','SUCCESSFUL','FAILED','PAID'));
CREATE INDEX idx_documents_applicant ON documents(applicant_id);
CREATE INDEX idx_payments_applicant ON payments(applicant_id);
CREATE INDEX idx_corrections_officer ON corrections(officer_id);
