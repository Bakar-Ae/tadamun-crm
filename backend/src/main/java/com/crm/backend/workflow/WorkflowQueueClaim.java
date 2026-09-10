package com.crm.backend.workflow;

public record WorkflowQueueClaim(Long actionExecutionId, String claimToken) {
}
