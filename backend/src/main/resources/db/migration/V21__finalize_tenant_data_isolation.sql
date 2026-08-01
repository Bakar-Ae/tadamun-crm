UPDATE teams team
JOIN organizations organization ON organization.slug = 'tadamun'
SET team.organization_id = organization.id
WHERE team.organization_id IS NULL;

UPDATE customers customer
JOIN organizations organization ON organization.slug = 'tadamun'
SET customer.organization_id = organization.id
WHERE customer.organization_id IS NULL;

UPDATE leads lead_record
JOIN organizations organization ON organization.slug = 'tadamun'
SET lead_record.organization_id = organization.id
WHERE lead_record.organization_id IS NULL;

UPDATE contacts contact
JOIN customers customer ON customer.id = contact.customer_id
SET contact.organization_id = customer.organization_id
WHERE contact.organization_id IS NULL;

UPDATE tasks task
LEFT JOIN customers customer ON customer.id = task.customer_id
LEFT JOIN leads lead_record ON lead_record.id = task.lead_id
JOIN organizations default_organization
    ON default_organization.slug = 'tadamun'
SET task.organization_id = COALESCE(
        customer.organization_id,
        lead_record.organization_id,
        default_organization.id
    )
WHERE task.organization_id IS NULL;

UPDATE notes note
LEFT JOIN customers customer ON customer.id = note.customer_id
LEFT JOIN leads lead_record ON lead_record.id = note.lead_id
JOIN organizations default_organization
    ON default_organization.slug = 'tadamun'
SET note.organization_id = COALESCE(
        customer.organization_id,
        lead_record.organization_id,
        default_organization.id
    )
WHERE note.organization_id IS NULL;

UPDATE attachments attachment
LEFT JOIN customers customer ON customer.id = attachment.customer_id
LEFT JOIN leads lead_record ON lead_record.id = attachment.lead_id
JOIN organizations default_organization
    ON default_organization.slug = 'tadamun'
SET attachment.organization_id = COALESCE(
        customer.organization_id,
        lead_record.organization_id,
        default_organization.id
    )
WHERE attachment.organization_id IS NULL;

ALTER TABLE teams
    MODIFY organization_id BIGINT NOT NULL;

ALTER TABLE customers
    MODIFY organization_id BIGINT NOT NULL;

ALTER TABLE contacts
    MODIFY organization_id BIGINT NOT NULL;

ALTER TABLE leads
    MODIFY organization_id BIGINT NOT NULL;

ALTER TABLE tasks
    MODIFY organization_id BIGINT NOT NULL;

ALTER TABLE notes
    MODIFY organization_id BIGINT NOT NULL;

ALTER TABLE attachments
    MODIFY organization_id BIGINT NOT NULL;
