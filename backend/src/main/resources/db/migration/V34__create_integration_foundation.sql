CREATE TABLE integration_connections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_connection_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    configuration JSON NOT NULL,
    external_account_id VARCHAR(255) NULL,
    credentials_updated_at TIMESTAMP NULL,
    last_verified_at TIMESTAMP NULL,
    last_error_category VARCHAR(60) NULL,
    last_error_message VARCHAR(500) NULL,
    created_by_user_id BIGINT NOT NULL,
    updated_by_user_id BIGINT NULL,
    revoked_by_user_id BIGINT NULL,
    revoked_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_integration_connections_public_id
        UNIQUE (public_connection_id),
    CONSTRAINT uq_integration_connections_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT uq_integration_connections_org_provider_name
        UNIQUE (organization_id, provider, name),
    CONSTRAINT fk_integration_connections_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_integration_connections_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_integration_connections_updated_by
        FOREIGN KEY (updated_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_integration_connections_revoked_by
        FOREIGN KEY (revoked_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_integration_connections_provider CHECK (
        provider IN ('WHATSAPP_CLOUD', 'SMTP')
    ),
    CONSTRAINT chk_integration_connections_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'DISABLED', 'ERROR', 'REVOKED')
    ),
    CONSTRAINT chk_integration_connections_revocation CHECK (
        (status = 'REVOKED' AND revoked_at IS NOT NULL
            AND revoked_by_user_id IS NOT NULL)
        OR (status <> 'REVOKED' AND revoked_at IS NULL
            AND revoked_by_user_id IS NULL)
    )
);

CREATE INDEX idx_integration_connections_org_status
    ON integration_connections(organization_id, status);

CREATE INDEX idx_integration_connections_org_provider
    ON integration_connections(organization_id, provider, status);

CREATE TABLE integration_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    connection_id BIGINT NOT NULL,
    ciphertext BLOB NOT NULL,
    nonce VARBINARY(12) NOT NULL,
    authentication_tag VARBINARY(16) NOT NULL,
    key_version VARCHAR(50) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_integration_credentials_connection
        UNIQUE (connection_id),
    CONSTRAINT uq_integration_credentials_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT fk_integration_credentials_connection
        FOREIGN KEY (connection_id, organization_id)
        REFERENCES integration_connections(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_integration_credentials_schema CHECK (
        schema_version >= 1
    )
);

CREATE INDEX idx_integration_credentials_org_connection
    ON integration_credentials(organization_id, connection_id);

INSERT INTO permissions (name, description) VALUES
    ('INTEGRATION_VIEW', 'View integration connections and health'),
    ('INTEGRATION_MANAGE', 'Manage integration connections and credentials');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN', 'MANAGER')
  AND permission.name = 'INTEGRATION_VIEW';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN')
  AND permission.name = 'INTEGRATION_MANAGE';
