ALTER TABLE workflow_action_attempts
    DROP CHECK chk_workflow_action_attempts_number;

ALTER TABLE workflow_action_attempts
    ADD CONSTRAINT chk_workflow_action_attempts_number
        CHECK (attempt_number >= 1);
