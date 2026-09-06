CREATE TABLE public_api_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    public_id VARCHAR(32) NOT NULL,
    display_prefix VARCHAR(60) NOT NULL,
    secret_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    expires_at DATETIME NULL,
    last_used_at DATETIME NULL,
    created_by_user_id BIGINT NOT NULL,
    revoked_by_user_id BIGINT NULL,
    revoked_at DATETIME NULL,
    rotated_from_key_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_public_api_keys_public_id UNIQUE (public_id),
    CONSTRAINT fk_public_api_keys_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_public_api_keys_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_public_api_keys_revoked_by
        FOREIGN KEY (revoked_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_public_api_keys_rotated_from
        FOREIGN KEY (rotated_from_key_id) REFERENCES public_api_keys(id),
    CONSTRAINT chk_public_api_keys_rate_limit
        CHECK (rate_limit_per_minute BETWEEN 1 AND 10000),
    INDEX idx_public_api_keys_organization_status
        (organization_id, status)
);

CREATE TABLE public_api_key_scopes (
    api_key_id BIGINT NOT NULL,
    scope_key VARCHAR(50) NOT NULL,

    PRIMARY KEY (api_key_id, scope_key),
    CONSTRAINT fk_public_api_key_scopes_key
        FOREIGN KEY (api_key_id)
            REFERENCES public_api_keys(id)
            ON DELETE CASCADE
);
