CREATE TABLE organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    time_zone VARCHAR(60) NOT NULL DEFAULT 'UTC',
    created_by_user_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_organizations_slug
        UNIQUE (slug),

    CONSTRAINT fk_organizations_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_organizations_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE INDEX idx_organizations_status
    ON organizations(status);

CREATE INDEX idx_organizations_created_by_user_id
    ON organizations(created_by_user_id);