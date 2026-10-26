ALTER TABLE app_users RENAME COLUMN username TO email;
ALTER TABLE app_users ALTER COLUMN email TYPE VARCHAR(254);

UPDATE app_users
SET email = LOWER(TRIM(email)) || '@permitworks.com'
WHERE POSITION('@' IN email) = 0;

UPDATE app_users SET email = LOWER(TRIM(email));

CREATE UNIQUE INDEX uq_app_users_email ON app_users (email);
ALTER TABLE app_users ADD CONSTRAINT ck_app_users_permitworks_email
 CHECK (email = LOWER(TRIM(email)) AND email LIKE '%@permitworks.com');
