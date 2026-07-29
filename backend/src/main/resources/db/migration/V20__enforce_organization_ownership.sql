ALTER TABLE teams
    DROP INDEX name,
    ADD CONSTRAINT fk_teams_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT uq_teams_organization_name
        UNIQUE (organization_id, name),
    ADD INDEX idx_teams_organization_status (organization_id, status);

ALTER TABLE customers
    ADD CONSTRAINT fk_customers_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_customers_organization_status
        (organization_id, status, created_at);

ALTER TABLE contacts
    ADD CONSTRAINT fk_contacts_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_contacts_organization_customer
        (organization_id, customer_id);

ALTER TABLE leads
    ADD CONSTRAINT fk_leads_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_leads_organization_status
        (organization_id, status, created_at);

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_tasks_organization_status_due
        (organization_id, status, due_date);

ALTER TABLE notes
    ADD CONSTRAINT fk_notes_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_notes_organization_customer_created
        (organization_id, customer_id, created_at),
    ADD INDEX idx_notes_organization_lead_created
        (organization_id, lead_id, created_at);

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_attachments_organization_customer
        (organization_id, customer_id, status, created_at),
    ADD INDEX idx_attachments_organization_lead
        (organization_id, lead_id, status, created_at);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD INDEX idx_notifications_organization_recipient
        (organization_id, recipient_user_id, read_status, created_at);

ALTER TABLE audit_logs
    MODIFY COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'PLATFORM',
    ADD CONSTRAINT fk_audit_logs_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_audit_logs_scope
        CHECK (
            (scope = 'PLATFORM' AND organization_id IS NULL)
            OR
            (scope = 'ORGANIZATION' AND organization_id IS NOT NULL)
        ),
    ADD INDEX idx_audit_logs_organization_created
        (organization_id, created_at);
