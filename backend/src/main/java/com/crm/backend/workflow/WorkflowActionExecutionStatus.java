package com.crm.backend.workflow;

public enum WorkflowActionExecutionStatus {
    PENDING,
    PROCESSING,
    RETRY_SCHEDULED,
    SUCCEEDED,
    FAILED,
    DEAD,
    SKIPPED
}
