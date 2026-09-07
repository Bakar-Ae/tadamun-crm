package com.crm.backend.workflow;

public enum WorkflowExecutionStatus {
    PENDING,
    PROCESSING,
    RETRY_SCHEDULED,
    SUCCEEDED,
    PARTIALLY_SUCCEEDED,
    FAILED,
    DEAD,
    CANCELLED
}
