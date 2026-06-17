ALTER TABLE users
ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users
SET password_change_required = TRUE
WHERE email = 'admin@crm.com';