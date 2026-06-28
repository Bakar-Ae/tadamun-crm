CREATE TABLE notification_preferences (
    user_id BIGINT PRIMARY KEY,

    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    task_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    customer_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    lead_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    report_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

INSERT INTO notification_preferences (user_id)
SELECT id
FROM users;