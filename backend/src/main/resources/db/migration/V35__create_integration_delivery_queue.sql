CREATE TABLE integration_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_delivery_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    connection_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    delivery_type VARCHAR(30) NOT NULL,
    destination VARCHAR(320) NOT NULL,
    subject VARCHAR(200) NULL,
    message_body TEXT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    maximum_attempts INT NOT NULL DEFAULT 6,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP NULL,
    claim_token VARCHAR(64) NULL,
    provider_message_id VARCHAR(255) NULL,
    last_error_category VARCHAR(60) NULL,
    last_error_message VARCHAR(500) NULL,
    created_by_user_id BIGINT NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_integration_deliveries_public_id
        UNIQUE (public_delivery_id),
    CONSTRAINT uq_integration_deliveries_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT uq_integration_deliveries_org_idempotency
        UNIQUE (organization_id, idempotency_key),
    CONSTRAINT fk_integration_deliveries_connection
        FOREIGN KEY (connection_id, organization_id)
        REFERENCES integration_connections(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_integration_deliveries_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_integration_deliveries_provider CHECK (
        provider IN ('WHATSAPP_CLOUD', 'SMTP')
    ),
    CONSTRAINT chk_integration_deliveries_type CHECK (
        delivery_type IN ('WHATSAPP_TEXT', 'EMAIL')
    ),
    CONSTRAINT chk_integration_deliveries_status CHECK (
        status IN (
            'PENDING', 'PROCESSING', 'RETRY_SCHEDULED', 'SUCCEEDED',
            'TERMINAL_FAILURE', 'DEAD', 'CANCELLED'
        )
    ),
    CONSTRAINT chk_integration_deliveries_attempts CHECK (
        attempt_count >= 0 AND maximum_attempts BETWEEN 1 AND 10
    ),
    CONSTRAINT chk_integration_deliveries_claim CHECK (
        (claimed_at IS NULL AND claim_token IS NULL)
        OR (claimed_at IS NOT NULL AND claim_token IS NOT NULL)
    )
);

CREATE INDEX idx_integration_deliveries_queue
    ON integration_deliveries(status, next_attempt_at, claimed_at);

CREATE INDEX idx_integration_deliveries_org_connection_created
    ON integration_deliveries(organization_id, connection_id, created_at);

CREATE TABLE integration_delivery_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    delivery_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    duration_ms INT NOT NULL,
    provider_message_id VARCHAR(255) NULL,
    error_category VARCHAR(60) NULL,
    error_message VARCHAR(500) NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_integration_delivery_attempts_number
        UNIQUE (delivery_id, attempt_number),
    CONSTRAINT fk_integration_delivery_attempts_delivery
        FOREIGN KEY (delivery_id, organization_id)
        REFERENCES integration_deliveries(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_integration_delivery_attempts_number_outcome CHECK (
        outcome IN ('SUCCEEDED', 'RETRYABLE_FAILURE', 'TERMINAL_FAILURE')
    ),
    CONSTRAINT chk_integration_delivery_attempts_duration CHECK (
        duration_ms >= 0
    )
);

CREATE INDEX idx_integration_delivery_attempts_org_delivery
    ON integration_delivery_attempts(
        organization_id,
        delivery_id,
        attempted_at
    );
