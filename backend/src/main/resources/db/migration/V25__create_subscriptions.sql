CREATE TABLE subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    trial_days INT NOT NULL DEFAULT 14,
    grace_period_days INT NOT NULL DEFAULT 7,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_subscription_plans_code UNIQUE (code),
    CONSTRAINT chk_subscription_plans_code CHECK (
        code IN ('STARTER', 'PROFESSIONAL', 'BUSINESS', 'ENTERPRISE')
    ),
    CONSTRAINT chk_subscription_plans_trial_days CHECK (trial_days >= 0),
    CONSTRAINT chk_subscription_plans_grace_days CHECK (
        grace_period_days >= 0
    )
);

CREATE TABLE subscription_plan_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    feature_key VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    limit_value BIGINT NULL,

    CONSTRAINT uq_subscription_plan_features_plan_feature
        UNIQUE (plan_id, feature_key),
    CONSTRAINT fk_subscription_plan_features_plan
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_subscription_plan_features_key CHECK (
        feature_key IN (
            'MEMBERS',
            'STORAGE_BYTES',
            'ADVANCED_REPORTING',
            'PUBLIC_API',
            'WEBHOOKS',
            'WORKFLOW_AUTOMATION',
            'PRIORITY_SUPPORT'
        )
    ),
    CONSTRAINT chk_subscription_plan_features_limit CHECK (
        limit_value IS NULL OR limit_value >= 0
    )
);

CREATE INDEX idx_subscription_plan_features_plan_id
    ON subscription_plan_features(plan_id);

CREATE TABLE organization_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    trial_ends_at TIMESTAMP NULL,
    current_period_starts_at TIMESTAMP NULL,
    current_period_ends_at TIMESTAMP NULL,
    grace_period_ends_at TIMESTAMP NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_organization_subscriptions_organization
        UNIQUE (organization_id),
    CONSTRAINT fk_organization_subscriptions_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_organization_subscriptions_plan
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_organization_subscriptions_status CHECK (
        status IN (
            'TRIALING',
            'ACTIVE',
            'PAST_DUE',
            'GRACE_PERIOD',
            'CANCELED',
            'EXPIRED'
        )
    ),
    CONSTRAINT chk_organization_subscriptions_trial CHECK (
        trial_ends_at IS NULL OR trial_ends_at >= started_at
    ),
    CONSTRAINT chk_organization_subscriptions_period CHECK (
        current_period_starts_at IS NULL
        OR current_period_ends_at IS NULL
        OR current_period_ends_at > current_period_starts_at
    )
);

CREATE INDEX idx_organization_subscriptions_plan_id
    ON organization_subscriptions(plan_id);

CREATE INDEX idx_organization_subscriptions_status
    ON organization_subscriptions(status);

INSERT INTO subscription_plans (
    code,
    name,
    description,
    trial_days,
    grace_period_days,
    active,
    display_order
) VALUES
    ('STARTER', 'Starter', 'Core CRM tools for small teams', 14, 7, TRUE, 1),
    ('PROFESSIONAL', 'Professional', 'Advanced tools for growing sales teams', 14, 7, TRUE, 2),
    ('BUSINESS', 'Business', 'Automation and integration tools for larger teams', 14, 14, TRUE, 3),
    ('ENTERPRISE', 'Enterprise', 'Expanded controls and support for complex organizations', 30, 30, TRUE, 4);

INSERT INTO subscription_plan_features (
    plan_id,
    feature_key,
    enabled,
    limit_value
)
SELECT plan.id, feature.feature_key, feature.enabled, feature.limit_value
FROM subscription_plans plan
JOIN (
    SELECT 'STARTER' plan_code, 'MEMBERS' feature_key, TRUE enabled, 5 limit_value
    UNION ALL SELECT 'STARTER', 'STORAGE_BYTES', TRUE, 1073741824
    UNION ALL SELECT 'STARTER', 'ADVANCED_REPORTING', FALSE, NULL
    UNION ALL SELECT 'STARTER', 'PUBLIC_API', FALSE, NULL
    UNION ALL SELECT 'STARTER', 'WEBHOOKS', FALSE, NULL
    UNION ALL SELECT 'STARTER', 'WORKFLOW_AUTOMATION', FALSE, NULL
    UNION ALL SELECT 'STARTER', 'PRIORITY_SUPPORT', FALSE, NULL
    UNION ALL SELECT 'PROFESSIONAL', 'MEMBERS', TRUE, 25
    UNION ALL SELECT 'PROFESSIONAL', 'STORAGE_BYTES', TRUE, 10737418240
    UNION ALL SELECT 'PROFESSIONAL', 'ADVANCED_REPORTING', TRUE, NULL
    UNION ALL SELECT 'PROFESSIONAL', 'PUBLIC_API', FALSE, NULL
    UNION ALL SELECT 'PROFESSIONAL', 'WEBHOOKS', TRUE, NULL
    UNION ALL SELECT 'PROFESSIONAL', 'WORKFLOW_AUTOMATION', TRUE, 10
    UNION ALL SELECT 'PROFESSIONAL', 'PRIORITY_SUPPORT', FALSE, NULL
    UNION ALL SELECT 'BUSINESS', 'MEMBERS', TRUE, 100
    UNION ALL SELECT 'BUSINESS', 'STORAGE_BYTES', TRUE, 107374182400
    UNION ALL SELECT 'BUSINESS', 'ADVANCED_REPORTING', TRUE, NULL
    UNION ALL SELECT 'BUSINESS', 'PUBLIC_API', TRUE, NULL
    UNION ALL SELECT 'BUSINESS', 'WEBHOOKS', TRUE, NULL
    UNION ALL SELECT 'BUSINESS', 'WORKFLOW_AUTOMATION', TRUE, 100
    UNION ALL SELECT 'BUSINESS', 'PRIORITY_SUPPORT', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'MEMBERS', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'STORAGE_BYTES', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'ADVANCED_REPORTING', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'PUBLIC_API', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'WEBHOOKS', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'WORKFLOW_AUTOMATION', TRUE, NULL
    UNION ALL SELECT 'ENTERPRISE', 'PRIORITY_SUPPORT', TRUE, NULL
) feature ON feature.plan_code = plan.code;

INSERT INTO organization_subscriptions (
    organization_id,
    plan_id,
    status,
    started_at,
    trial_ends_at,
    cancel_at_period_end
)
SELECT
    organization.id,
    plan.id,
    'TRIALING',
    CURRENT_TIMESTAMP,
    DATE_ADD(CURRENT_TIMESTAMP, INTERVAL plan.trial_days DAY),
    FALSE
FROM organizations organization
JOIN subscription_plans plan ON plan.code = 'STARTER'
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_subscriptions subscription
    WHERE subscription.organization_id = organization.id
);

INSERT INTO permissions (name, description) VALUES
    ('SUBSCRIPTION_VIEW', 'View organization subscription and plans'),
    ('SUBSCRIPTION_MANAGE', 'Manage organization subscription');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN', 'MANAGER', 'SALES_REP', 'SUPPORT_STAFF')
  AND permission.name = 'SUBSCRIPTION_VIEW';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN')
  AND permission.name = 'SUBSCRIPTION_MANAGE';
