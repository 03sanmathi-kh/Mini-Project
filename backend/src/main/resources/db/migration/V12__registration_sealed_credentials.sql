-- Sealed delivery of Admin-created login credentials to the applicant.
-- At registration an RSA key pair is created; the private key is stored only in encrypted form
-- (AES-GCM, key derived from the applicant's registration password). On approval the Admin-entered
-- login password is encrypted with the public key, so only the registration password can reveal it.
-- No plaintext password is stored. Additive only: existing rows keep NULLs.
ALTER TABLE registration_requests ADD COLUMN credential_public_key VARCHAR(1000);
ALTER TABLE registration_requests ADD COLUMN credential_private_key_enc VARCHAR(4000);
ALTER TABLE registration_requests ADD COLUMN credential_kdf_salt VARCHAR(64);
ALTER TABLE registration_requests ADD COLUMN credential_iv VARCHAR(64);
ALTER TABLE registration_requests ADD COLUMN sealed_login_password VARCHAR(1000);
