UPDATE auth_sessions SET last_activity = created_at WHERE last_activity IS NULL;
ALTER TABLE auth_sessions ALTER COLUMN last_activity SET NOT NULL;
