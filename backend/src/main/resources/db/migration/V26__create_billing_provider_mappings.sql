ALTER TABLE organization_subscriptions
    ADD COLUMN billing_provider VARCHAR(30) NULL AFTER plan_id,
    ADD COLUMN provider_subscription_id VARCHAR(255) NULL
        AFTER billing_provider,
    ADD COLUMN provider_status_updated_at TIMESTAMP NULL
        AFTER provider_subscription_id,
    ADD CONSTRAINT uq_org_subscriptions_provider_reference
        UNIQUE (billing_provider, provider_subscription_id);

CREATE TABLE billing_customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_customer_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_billing_customers_org_provider
        UNIQUE (organization_id, provider),
    CONSTRAINT uq_billing_customers_provider_reference
        UNIQUE (provider, provider_customer_id),
    CONSTRAINT fk_billing_customers_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT
);

CREATE TABLE billing_plan_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_price_id VARCHAR(255) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_billing_plan_prices_plan_interval
        UNIQUE (plan_id, provider, billing_interval),
    CONSTRAINT uq_billing_plan_prices_provider_reference
        UNIQUE (provider, provider_price_id),
    CONSTRAINT fk_billing_plan_prices_plan
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_billing_plan_prices_interval CHECK (
        billing_interval IN ('MONTHLY', 'YEARLY')
    )
);

CREATE TABLE billing_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    organization_id BIGINT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    payload_sha256 CHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,

    CONSTRAINT uq_billing_webhook_events_provider_event
        UNIQUE (provider, provider_event_id),
    CONSTRAINT fk_billing_webhook_events_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_billing_webhook_processing_status CHECK (
        processing_status IN (
            'RECEIVED',
            'PROCESSED',
            'FAILED',
            'IGNORED'
        )
    ),
    CONSTRAINT chk_billing_webhook_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_billing_customers_organization
    ON billing_customers(organization_id);

CREATE INDEX idx_billing_plan_prices_plan
    ON billing_plan_prices(plan_id);

CREATE INDEX idx_billing_webhook_events_status
    ON billing_webhook_events(processing_status);
