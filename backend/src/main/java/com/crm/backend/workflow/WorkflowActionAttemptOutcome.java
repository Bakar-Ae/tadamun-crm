package com.crm.backend.workflow;

public enum WorkflowActionAttemptOutcome {
    SUCCEEDED,
    RETRYABLE_FAILURE,
    TERMINAL_FAILURE
}
