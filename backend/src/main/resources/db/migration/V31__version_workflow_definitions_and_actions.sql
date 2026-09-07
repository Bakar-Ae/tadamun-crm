ALTER TABLE workflow_definitions
    ADD COLUMN definition_version BIGINT NOT NULL DEFAULT 1
        AFTER updated_by_user_id,
    ADD CONSTRAINT chk_workflow_definitions_definition_version
        CHECK (definition_version >= 1);

ALTER TABLE workflow_actions
    DROP INDEX uq_workflow_actions_workflow_order,
    ADD COLUMN definition_version BIGINT NOT NULL DEFAULT 1
        AFTER workflow_id,
    ADD COLUMN retired_at TIMESTAMP NULL
        AFTER timeout_seconds,
    ADD CONSTRAINT uq_workflow_actions_workflow_version_order
        UNIQUE (workflow_id, definition_version, action_order),
    ADD CONSTRAINT chk_workflow_actions_definition_version
        CHECK (definition_version >= 1);

ALTER TABLE workflow_executions
    DROP CHECK chk_workflow_executions_version,
    ADD CONSTRAINT chk_workflow_executions_version
        CHECK (workflow_version >= 1);

CREATE INDEX idx_workflow_actions_current_version
    ON workflow_actions(workflow_id, definition_version, retired_at,
                        action_order);
