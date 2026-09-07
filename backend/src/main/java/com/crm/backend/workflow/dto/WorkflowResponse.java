package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowFailurePolicy;
import com.crm.backend.workflow.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowResponse(
        Long id,
        String publicWorkflowId,
        String name,
        String description,
        WorkflowStatus status,
        WorkflowFailurePolicy failurePolicy,
        int executionTimeoutSeconds,
        int maximumAttempts,
        int maximumExecutionsPerHour,
        long definitionVersion,
        WorkflowTriggerResponse trigger,
        List<WorkflowActionResponse> actions,
        Long createdByUserId,
        String createdByName,
        Long updatedByUserId,
        String updatedByName,
        LocalDateTime activatedAt,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
