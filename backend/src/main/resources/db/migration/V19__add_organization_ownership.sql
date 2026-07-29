ALTER TABLE teams
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE customers
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE contacts
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE leads
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE tasks
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE notes
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE attachments
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE notifications
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE audit_logs
    ADD COLUMN organization_id BIGINT NULL AFTER id,
    ADD COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'ORGANIZATION'
        AFTER organization_id;

UPDATE teams t
JOIN organizations o ON o.slug = 'tadamun'
SET t.organization_id = o.id
WHERE t.organization_id IS NULL;

UPDATE customers c
JOIN organizations o ON o.slug = 'tadamun'
SET c.organization_id = o.id
WHERE c.organization_id IS NULL;

UPDATE contacts c
JOIN organizations o ON o.slug = 'tadamun'
SET c.organization_id = o.id
WHERE c.organization_id IS NULL;

UPDATE leads l
JOIN organizations o ON o.slug = 'tadamun'
SET l.organization_id = o.id
WHERE l.organization_id IS NULL;

UPDATE tasks t
JOIN organizations o ON o.slug = 'tadamun'
SET t.organization_id = o.id
WHERE t.organization_id IS NULL;

UPDATE notes n
JOIN organizations o ON o.slug = 'tadamun'
SET n.organization_id = o.id
WHERE n.organization_id IS NULL;

UPDATE attachments a
JOIN organizations o ON o.slug = 'tadamun'
SET a.organization_id = o.id
WHERE a.organization_id IS NULL;

UPDATE notifications n
JOIN organizations o ON o.slug = 'tadamun'
SET n.organization_id = o.id
WHERE n.organization_id IS NULL;

UPDATE audit_logs a
JOIN organizations o ON o.slug = 'tadamun'
SET a.organization_id = o.id,
    a.scope = 'ORGANIZATION'
WHERE a.organization_id IS NULL;