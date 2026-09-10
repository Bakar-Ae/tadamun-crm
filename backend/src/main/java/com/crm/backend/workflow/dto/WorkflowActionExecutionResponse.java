package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowActionExecutionStatus;
import com.crm.backend.workflow.WorkflowActionType;

import java.time.LocalDateTime;

public record WorkflowActionExecutionResponse(
        String publicActionExecutionId,
        short actionOrder,
        String name,
        WorkflowActionType actionType,
        WorkflowActionExecutionStatus status,
        int attemptCount,
        String resultResourceType,
        Long resultResourceId,
        String lastErrorCategory,
        String lastError,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
