ALTER TABLE workflow_executions
    DROP CHECK chk_workflow_executions_attempts;

ALTER TABLE workflow_executions
    ADD CONSTRAINT chk_workflow_executions_attempts
        CHECK (attempt_count >= 0);

ALTER TABLE workflow_action_executions
    DROP CHECK chk_workflow_action_executions_attempts;

ALTER TABLE workflow_action_executions
    ADD CONSTRAINT chk_workflow_action_executions_attempts
        CHECK (attempt_count >= 0);
