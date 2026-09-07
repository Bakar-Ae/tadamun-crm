CREATE TABLE workflow_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_workflow_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    failure_policy VARCHAR(30) NOT NULL DEFAULT 'STOP_ON_FAILURE',
    execution_timeout_seconds INT NOT NULL DEFAULT 60,
    maximum_attempts INT NOT NULL DEFAULT 3,
    maximum_executions_per_hour INT NOT NULL DEFAULT 100,
    activated_at TIMESTAMP NULL,
    archived_at TIMESTAMP NULL,
    created_by_user_id BIGINT NOT NULL,
    updated_by_user_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_workflow_definitions_public_id
        UNIQUE (public_workflow_id),
    CONSTRAINT uq_workflow_definitions_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT uq_workflow_definitions_org_name
        UNIQUE (organization_id, name),
    CONSTRAINT fk_workflow_definitions_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_workflow_definitions_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_workflow_definitions_updated_by
        FOREIGN KEY (updated_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_workflow_definitions_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED')
    ),
    CONSTRAINT chk_workflow_definitions_failure_policy CHECK (
        failure_policy IN ('STOP_ON_FAILURE', 'CONTINUE_ON_FAILURE')
    ),
    CONSTRAINT chk_workflow_definitions_timeout CHECK (
        execution_timeout_seconds BETWEEN 5 AND 300
    ),
    CONSTRAINT chk_workflow_definitions_attempts CHECK (
        maximum_attempts BETWEEN 1 AND 5
    ),
    CONSTRAINT chk_workflow_definitions_hourly_limit CHECK (
        maximum_executions_per_hour BETWEEN 1 AND 10000
    ),
    CONSTRAINT chk_workflow_definitions_lifecycle CHECK (
        (status = 'ACTIVE' AND activated_at IS NOT NULL
            AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
        OR (status IN ('DRAFT', 'PAUSED') AND archived_at IS NULL)
    )
);

CREATE INDEX idx_workflow_definitions_org_status
    ON workflow_definitions(organization_id, status);

CREATE TABLE workflow_triggers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    trigger_type VARCHAR(30) NOT NULL DEFAULT 'CRM_EVENT',
    event_type VARCHAR(100) NOT NULL,
    condition_config JSON NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_workflow_triggers_workflow UNIQUE (workflow_id),
    CONSTRAINT uq_workflow_triggers_id_org UNIQUE (id, organization_id),
    CONSTRAINT fk_workflow_triggers_workflow
        FOREIGN KEY (workflow_id, organization_id)
        REFERENCES workflow_definitions(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_workflow_triggers_type CHECK (
        trigger_type = 'CRM_EVENT'
    ),
    CONSTRAINT chk_workflow_triggers_event_type CHECK (
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

CREATE INDEX idx_workflow_triggers_org_event_enabled
    ON workflow_triggers(organization_id, event_type, enabled);

CREATE TABLE workflow_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_action_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    action_order SMALLINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    configuration JSON NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    timeout_seconds INT NOT NULL DEFAULT 15,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_workflow_actions_public_id UNIQUE (public_action_id),
    CONSTRAINT uq_workflow_actions_id_org UNIQUE (id, organization_id),
    CONSTRAINT uq_workflow_actions_workflow_order
        UNIQUE (workflow_id, action_order),
    CONSTRAINT fk_workflow_actions_workflow
        FOREIGN KEY (workflow_id, organization_id)
        REFERENCES workflow_definitions(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_workflow_actions_order CHECK (
        action_order BETWEEN 1 AND 10
    ),
    CONSTRAINT chk_workflow_actions_type CHECK (
        action_type IN ('CREATE_TASK', 'SEND_IN_APP_NOTIFICATION')
    ),
    CONSTRAINT chk_workflow_actions_timeout CHECK (
        timeout_seconds BETWEEN 1 AND 60
    )
);

CREATE INDEX idx_workflow_actions_workflow_enabled_order
    ON workflow_actions(workflow_id, enabled, action_order);

CREATE TABLE workflow_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_execution_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    source_event_id BIGINT NOT NULL,
    parent_execution_id BIGINT NULL,
    workflow_version BIGINT NOT NULL,
    trigger_event_type VARCHAR(100) NOT NULL,
    source_event_public_id VARCHAR(40) NOT NULL,
    correlation_id VARCHAR(40) NOT NULL,
    automation_depth SMALLINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP NULL,
    claim_token VARCHAR(64) NULL,
    current_action_order SMALLINT NULL,
    input_payload JSON NOT NULL,
    last_error_category VARCHAR(60) NULL,
    last_error VARCHAR(500) NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_workflow_executions_public_id
        UNIQUE (public_execution_id),
    CONSTRAINT uq_workflow_executions_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT uq_workflow_executions_workflow_event
        UNIQUE (organization_id, workflow_id, source_event_id),
    CONSTRAINT fk_workflow_executions_workflow
        FOREIGN KEY (workflow_id, organization_id)
        REFERENCES workflow_definitions(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_workflow_executions_source_event
        FOREIGN KEY (source_event_id, organization_id)
        REFERENCES webhook_events(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_workflow_executions_parent
        FOREIGN KEY (parent_execution_id, organization_id)
        REFERENCES workflow_executions(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_workflow_executions_status CHECK (
        status IN (
            'PENDING',
            'PROCESSING',
            'RETRY_SCHEDULED',
            'SUCCEEDED',
            'PARTIALLY_SUCCEEDED',
            'FAILED',
            'DEAD',
            'CANCELLED'
        )
    ),
    CONSTRAINT chk_workflow_executions_version CHECK (
        workflow_version >= 0
    ),
    CONSTRAINT chk_workflow_executions_depth CHECK (
        automation_depth BETWEEN 0 AND 5
    ),
    CONSTRAINT chk_workflow_executions_attempts CHECK (
        attempt_count BETWEEN 0 AND 5
    ),
    CONSTRAINT chk_workflow_executions_action_order CHECK (
        current_action_order IS NULL
        OR current_action_order BETWEEN 1 AND 10
    ),
    CONSTRAINT chk_workflow_executions_claim CHECK (
        (claimed_at IS NULL AND claim_token IS NULL)
        OR (claimed_at IS NOT NULL AND claim_token IS NOT NULL)
    )
);

CREATE INDEX idx_workflow_executions_queue
    ON workflow_executions(status, next_attempt_at, claimed_at);

CREATE INDEX idx_workflow_executions_org_created
    ON workflow_executions(organization_id, created_at);

CREATE INDEX idx_workflow_executions_correlation
    ON workflow_executions(organization_id, correlation_id, created_at);

CREATE TABLE workflow_action_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_action_execution_id VARCHAR(40) NOT NULL,
    organization_id BIGINT NOT NULL,
    execution_id BIGINT NOT NULL,
    action_id BIGINT NOT NULL,
    action_order SMALLINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    configuration_snapshot JSON NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NULL,
    claimed_at TIMESTAMP NULL,
    claim_token VARCHAR(64) NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    result_resource_type VARCHAR(50) NULL,
    result_resource_id BIGINT NULL,
    result_summary JSON NULL,
    last_error_category VARCHAR(60) NULL,
    last_error VARCHAR(500) NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_workflow_action_executions_public_id
        UNIQUE (public_action_execution_id),
    CONSTRAINT uq_workflow_action_executions_id_org
        UNIQUE (id, organization_id),
    CONSTRAINT uq_workflow_action_executions_execution_action
        UNIQUE (execution_id, action_id),
    CONSTRAINT uq_workflow_action_executions_idempotency
        UNIQUE (idempotency_key),
    CONSTRAINT fk_workflow_action_executions_execution
        FOREIGN KEY (execution_id, organization_id)
        REFERENCES workflow_executions(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_workflow_action_executions_action
        FOREIGN KEY (action_id, organization_id)
        REFERENCES workflow_actions(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_workflow_action_executions_order CHECK (
        action_order BETWEEN 1 AND 10
    ),
    CONSTRAINT chk_workflow_action_executions_type CHECK (
        action_type IN ('CREATE_TASK', 'SEND_IN_APP_NOTIFICATION')
    ),
    CONSTRAINT chk_workflow_action_executions_status CHECK (
        status IN (
            'PENDING',
            'PROCESSING',
            'RETRY_SCHEDULED',
            'SUCCEEDED',
            'FAILED',
            'DEAD',
            'SKIPPED'
        )
    ),
    CONSTRAINT chk_workflow_action_executions_attempts CHECK (
        attempt_count BETWEEN 0 AND 5
    ),
    CONSTRAINT chk_workflow_action_executions_claim CHECK (
        (claimed_at IS NULL AND claim_token IS NULL)
        OR (claimed_at IS NOT NULL AND claim_token IS NOT NULL)
    ),
    CONSTRAINT chk_workflow_action_executions_result CHECK (
        (result_resource_type IS NULL AND result_resource_id IS NULL)
        OR (result_resource_type IS NOT NULL AND result_resource_id IS NOT NULL)
    )
);

CREATE INDEX idx_workflow_action_executions_queue
    ON workflow_action_executions(status, next_attempt_at, claimed_at);

CREATE INDEX idx_workflow_action_executions_execution_order
    ON workflow_action_executions(execution_id, action_order);

CREATE TABLE workflow_action_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    action_execution_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    duration_ms INT NOT NULL,
    result_summary JSON NULL,
    error_category VARCHAR(60) NULL,
    error_message VARCHAR(500) NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_workflow_action_attempts_number
        UNIQUE (action_execution_id, attempt_number),
    CONSTRAINT fk_workflow_action_attempts_execution
        FOREIGN KEY (action_execution_id, organization_id)
        REFERENCES workflow_action_executions(id, organization_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_workflow_action_attempts_number CHECK (
        attempt_number BETWEEN 1 AND 5
    ),
    CONSTRAINT chk_workflow_action_attempts_outcome CHECK (
        outcome IN ('SUCCEEDED', 'RETRYABLE_FAILURE', 'TERMINAL_FAILURE')
    ),
    CONSTRAINT chk_workflow_action_attempts_duration CHECK (
        duration_ms >= 0
    )
);

CREATE INDEX idx_workflow_action_attempts_org_attempted
    ON workflow_action_attempts(organization_id, attempted_at);

INSERT INTO permissions (name, description) VALUES
    ('WORKFLOW_VIEW', 'View workflow definitions and execution history'),
    ('WORKFLOW_MANAGE', 'Manage workflows and execution recovery');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN', 'MANAGER')
  AND permission.name = 'WORKFLOW_VIEW';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('OWNER', 'ADMIN')
  AND permission.name = 'WORKFLOW_MANAGE';
