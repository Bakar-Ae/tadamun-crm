CREATE TABLE organization_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    email VARCHAR(190) NOT NULL,
    role_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    pending_email VARCHAR(190)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'PENDING' THEN LOWER(email)
                ELSE NULL
            END
        ) STORED,
    invited_by_user_id BIGINT NOT NULL,
    accepted_by_user_id BIGINT NULL,
    revoked_by_user_id BIGINT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_organization_invitations_token_hash
        UNIQUE (token_hash),

    CONSTRAINT uq_organization_invitations_pending_email
        UNIQUE (organization_id, pending_email),

    CONSTRAINT fk_invitations_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invitations_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invitations_invited_by
        FOREIGN KEY (invited_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invitations_accepted_by
        FOREIGN KEY (accepted_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invitations_revoked_by
        FOREIGN KEY (revoked_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_organization_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_invitations_organization_status
    ON organization_invitations(organization_id, status);

CREATE INDEX idx_invitations_email_status
    ON organization_invitations(email, status);

CREATE INDEX idx_invitations_expiration
    ON organization_invitations(expires_at);
