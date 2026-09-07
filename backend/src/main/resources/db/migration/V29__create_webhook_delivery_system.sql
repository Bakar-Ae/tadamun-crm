CREATE TABLE webhook_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    endpoint_url VARCHAR(2048) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    secret_display_suffix VARCHAR(8) NULL,
    current_secret_ciphertext BLOB NULL,
    current_secret_nonce VARBINARY(12) NULL,
    current_secret_tag VARBINARY(16) NULL,
    current_secret_key_version VARCHAR(50) NULL,
    previous_secret_ciphertext BLOB NULL,
    previous_secret_nonce VARBINARY(12) NULL,
    previous_secret_tag VARBINARY(16) NULL,
    previous_secret_key_version VARCHAR(50) NULL,
    previous_secret_expires_at TIMESTAMP NULL,
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_success_at TIMESTAMP NULL,
    last_failure_at TIMESTAMP NULL,
    created_by_user_id BIGINT NOT NULL,
    updated_by_user_id BIGINT NULL,
    revoked_by_user_id BIGINT NULL,
    revoked_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_webhook_subscriptions_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT fk_webhook_subscriptions_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_webhook_subscriptions_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_webhook_subscriptions_updated_by
        FOREIGN KEY (updated_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_webhook_subscriptions_revoked_by
        FOREIGN KEY (revoked_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_webhook_subscriptions_status CHECK (
        status IN ('ACTIVE', 'DISABLED', 'FAILED', 'REVOKED')
    ),
    CONSTRAINT chk_webhook_subscriptions_failures CHECK (
        consecutive_failures >= 0
    ),
    CONSTRAINT chk_webhook_subscriptions_current_secret CHECK (
        (
            status = 'REVOKED'
            AND current_secret_ciphertext IS NULL
            AND current_secret_nonce IS NULL
            AND current_secret_tag IS NULL
            AND current_secret_key_version IS NULL
        )
        OR (
            status <> 'REVOKED'
            AND current_secret_ciphertext IS NOT NULL
            AND current_secret_nonce IS NOT NULL
            AND current_secret_tag IS NOT NULL
            AND current_secret_key_version IS NOT NULL
        )
    ),
    CONSTRAINT chk_webhook_subscriptions_previous_secret CHECK (
        (
            previous_secret_ciphertext IS NULL
            AND previous_secret_nonce IS NULL
            AND previous_secret_tag IS NULL
            AND previous_secret_key_version IS NULL
            AND previous_secret_expires_at IS NULL
        )
        OR (
            previous_secret_ciphertext IS NOT NULL
            AND previous_secret_nonce IS NOT NULL
            AND previous_secret_tag IS NOT NULL
            AND previous_secret_key_version IS NOT NULL
            AND previous_secret_expires_at IS NOT NULL
        )
    ),
    CONSTRAINT chk_webhook_subscriptions_revocation CHECK (
        (status = 'REVOKED' AND revoked_at IS NOT NULL)
        OR (status <> 'REVOKED' AND revoked_at IS NULL)
    )
);

CREATE INDEX idx_webhook_subscriptions_org_status
    ON webhook_subscriptions(organization_id, status);

CREATE TABLE webhook_subscription_events (
    subscription_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (subscription_id, event_type),
    CONSTRAINT fk_webhook_subscription_events_subscription
        FOREIGN KEY (subscription_id, organization_id)
        REFERENCES webhook_subscriptions(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_webhook_subscription_events_type CHECK (
        event_type IN (
            'customer.created',
            'customer.updated',
            'customer.archived',
            'customer.restored',
            'lead.created',
            'lead.updated',
            'lead.archived',
            'lead.converted',
            'contact.created',
            'contact.updated',
            'task.created',
            'task.updated',
            'task.completed',
            'note.created'
        )
    )
);

CREATE INDEX idx_webhook_subscription_events_org_type
    ON webhook_subscription_events(organization_id, event_type);

CREATE TABLE webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_event_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload JSON NOT NULL,
    publication_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    publication_attempts INT NOT NULL DEFAULT 0,
    next_publication_attempt_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP NULL,
    claim_token VARCHAR(64) NULL,
    last_error VARCHAR(500) NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_webhook_events_public_id
        UNIQUE (public_event_id),
    CONSTRAINT uq_webhook_events_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT fk_webhook_events_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_webhook_events_type CHECK (
        event_type IN (
            'customer.created',
            'customer.updated',
            'customer.archived',
            'customer.restored',
            'lead.created',
            'lead.updated',
            'lead.archived',
            'lead.converted',
            'contact.created',
            'contact.updated',
            'task.created',
            'task.updated',
            'task.completed',
            'note.created'
        )
    ),
    CONSTRAINT chk_webhook_events_schema_version CHECK (
        schema_version >= 1
    ),
    CONSTRAINT chk_webhook_events_publication_status CHECK (
        publication_status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')
    ),
    CONSTRAINT chk_webhook_events_publication_attempts CHECK (
        publication_attempts >= 0
    )
);

CREATE INDEX idx_webhook_events_publication_queue
    ON webhook_events(
        publication_status,
        next_publication_attempt_at,
        claimed_at
    );

CREATE INDEX idx_webhook_events_org_occurred
    ON webhook_events(organization_id, occurred_at);

CREATE INDEX idx_webhook_events_aggregate
    ON webhook_events(
        organization_id,
        aggregate_type,
        aggregate_id,
        occurred_at
    );

CREATE TABLE webhook_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_delivery_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP NULL,
    claim_token VARCHAR(64) NULL,
    last_http_status INT NULL,
    last_duration_ms INT NULL,
    last_error_category VARCHAR(60) NULL,
    last_error VARCHAR(500) NULL,
    last_response_excerpt TEXT NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_webhook_deliveries_public_id
        UNIQUE (public_delivery_id),
    CONSTRAINT uq_webhook_deliveries_event_subscription
        UNIQUE (event_id, subscription_id),
    CONSTRAINT uq_webhook_deliveries_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT fk_webhook_deliveries_event
        FOREIGN KEY (event_id, organization_id)
        REFERENCES webhook_events(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_webhook_deliveries_subscription
        FOREIGN KEY (subscription_id, organization_id)
        REFERENCES webhook_subscriptions(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_webhook_deliveries_status CHECK (
        status IN (
            'PENDING',
            'PROCESSING',
            'SUCCEEDED',
            'RETRY_SCHEDULED',
            'TERMINAL_FAILURE',
            'DEAD'
        )
    ),
    CONSTRAINT chk_webhook_deliveries_attempt_count CHECK (
        attempt_count >= 0
    ),
    CONSTRAINT chk_webhook_deliveries_http_status CHECK (
        last_http_status IS NULL
        OR last_http_status BETWEEN 100 AND 599
    ),
    CONSTRAINT chk_webhook_deliveries_duration CHECK (
        last_duration_ms IS NULL OR last_duration_ms >= 0
    )
);

CREATE INDEX idx_webhook_deliveries_retry_queue
    ON webhook_deliveries(status, next_attempt_at, claimed_at);

CREATE INDEX idx_webhook_deliveries_org_created
    ON webhook_deliveries(organization_id, created_at);

CREATE INDEX idx_webhook_deliveries_subscription_created
    ON webhook_deliveries(subscription_id, created_at);

CREATE TABLE webhook_delivery_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    delivery_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    request_timestamp BIGINT NOT NULL,
    http_status INT NULL,
    duration_ms INT NOT NULL,
    response_excerpt TEXT NULL,
    error_category VARCHAR(60) NULL,
    error_message VARCHAR(500) NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_webhook_delivery_attempts_number
        UNIQUE (delivery_id, attempt_number),
    CONSTRAINT fk_webhook_delivery_attempts_delivery
        FOREIGN KEY (delivery_id, organization_id)
        REFERENCES webhook_deliveries(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_webhook_delivery_attempts_number CHECK (
        attempt_number >= 1
    ),
    CONSTRAINT chk_webhook_delivery_attempts_outcome CHECK (
        outcome IN ('SUCCEEDED', 'RETRYABLE_FAILURE', 'TERMINAL_FAILURE')
    ),
    CONSTRAINT chk_webhook_delivery_attempts_timestamp CHECK (
        request_timestamp > 0
    ),
    CONSTRAINT chk_webhook_delivery_attempts_http_status CHECK (
        http_status IS NULL OR http_status BETWEEN 100 AND 599
    ),
    CONSTRAINT chk_webhook_delivery_attempts_duration CHECK (
        duration_ms >= 0
    )
);

CREATE INDEX idx_webhook_delivery_attempts_org_attempted
    ON webhook_delivery_attempts(organization_id, attempted_at);

INSERT INTO permissions (name, description) VALUES
    ('WEBHOOK_VIEW', 'View webhook subscriptions and delivery history'),
    ('WEBHOOK_MANAGE', 'Manage webhook subscriptions and replay deliveries');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN', 'MANAGER')
  AND permission.name = 'WEBHOOK_VIEW';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN')
  AND permission.name = 'WEBHOOK_MANAGE';
