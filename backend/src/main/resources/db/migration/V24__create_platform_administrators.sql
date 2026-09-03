CREATE TABLE platform_administrators (
    user_id BIGINT PRIMARY KEY,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    granted_by_user_id BIGINT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,

    CONSTRAINT fk_platform_administrators_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_platform_administrators_granted_by
        FOREIGN KEY (granted_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_platform_administrators_status
        CHECK (status IN ('ACTIVE', 'REVOKED'))
);

CREATE INDEX idx_platform_administrators_status
    ON platform_administrators(status);

INSERT INTO platform_administrators (
    user_id,
    status,
    granted_by_user_id,
    granted_at
)
SELECT
    candidate_user.id,
    'ACTIVE',
    NULL,
    CURRENT_TIMESTAMP
FROM users candidate_user
JOIN roles legacy_role ON legacy_role.id = candidate_user.role_id
WHERE legacy_role.name = 'ADMIN'
  AND candidate_user.status = 'ACTIVE'
ORDER BY candidate_user.id
LIMIT 1;
