ALTER TABLE notifications
    ADD COLUMN deduplication_key VARCHAR(150) NULL AFTER type,
    ADD CONSTRAINT uq_notifications_organization_recipient_deduplication
        UNIQUE (organization_id, recipient_user_id, deduplication_key);
