ALTER TABLE applications ADD CONSTRAINT uq_application_applicant UNIQUE(application_id,applicant_id);
ALTER TABLE documents ADD CONSTRAINT fk_document_application_owner FOREIGN KEY(application_id,applicant_id) REFERENCES applications(application_id,applicant_id);
ALTER TABLE payments ADD CONSTRAINT fk_payment_application_owner FOREIGN KEY(application_id,applicant_id) REFERENCES applications(application_id,applicant_id);
