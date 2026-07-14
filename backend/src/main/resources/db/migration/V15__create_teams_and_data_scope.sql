ALTER TABLE roles
    ADD COLUMN data_scope VARCHAR(20) NOT NULL DEFAULT 'OWN'
        AFTER description,
    ADD CONSTRAINT chk_roles_data_scope
        CHECK (data_scope IN ('OWN', 'TEAM', 'ALL'));

UPDATE roles
SET data_scope = CASE name
    WHEN 'ADMIN' THEN 'ALL'
    WHEN 'MANAGER' THEN 'TEAM'
    ELSE 'OWN'
END;

CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    manager_user_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_teams_manager_user
        FOREIGN KEY (manager_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_teams_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_teams_manager_user_id
    ON teams(manager_user_id);

ALTER TABLE users
    ADD COLUMN team_id BIGINT NULL AFTER role_id,
    ADD CONSTRAINT fk_users_team
        FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE SET NULL;

CREATE INDEX idx_users_team_id
    ON users(team_id);

INSERT INTO teams (
    name,
    description,
    status
)
VALUES (
    'General',
    'Default team for existing CRM users',
    'ACTIVE'
);

UPDATE users
SET team_id = (
    SELECT id
    FROM teams
    WHERE name = 'General'
)
WHERE team_id IS NULL;

ALTER TABLE customers
    ADD COLUMN owner_user_id BIGINT NULL AFTER customer_type,
    ADD CONSTRAINT fk_customers_owner_user
        FOREIGN KEY (owner_user_id)
        REFERENCES users(id);

CREATE INDEX idx_customers_owner_user_id
    ON customers(owner_user_id);

UPDATE customers c
SET c.owner_user_id = (
    SELECT l.assigned_to_user_id
    FROM leads l
    WHERE l.converted_customer_id = c.id
      AND l.assigned_to_user_id IS NOT NULL
    ORDER BY l.id
    LIMIT 1
)
WHERE c.owner_user_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM leads l
      WHERE l.converted_customer_id = c.id
        AND l.assigned_to_user_id IS NOT NULL
  );

UPDATE customers c
JOIN (
    SELECT u.id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE r.name = 'ADMIN'
      AND u.status = 'ACTIVE'
    ORDER BY u.id
    LIMIT 1
) fallback_owner ON 1 = 1
SET c.owner_user_id = fallback_owner.id
WHERE c.owner_user_id IS NULL;

UPDATE leads l
JOIN (
    SELECT u.id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE r.name = 'ADMIN'
      AND u.status = 'ACTIVE'
    ORDER BY u.id
    LIMIT 1
) fallback_owner ON 1 = 1
SET l.assigned_to_user_id = fallback_owner.id
WHERE l.assigned_to_user_id IS NULL;

UPDATE tasks t
JOIN (
    SELECT u.id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE r.name = 'ADMIN'
      AND u.status = 'ACTIVE'
    ORDER BY u.id
    LIMIT 1
) fallback_owner ON 1 = 1
SET t.assigned_to_user_id = fallback_owner.id
WHERE t.assigned_to_user_id IS NULL;