package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowExecutionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowExecutionResponse(
        String publicExecutionId,
        Long workflowId,
        String workflowName,
        long workflowVersion,
        String triggerEventType,
        String sourceEventPublicId,
        String correlationId,
        WorkflowExecutionStatus status,
        int attemptCount,
        Short currentActionOrder,
        String lastErrorCategory,
        String lastError,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<WorkflowActionExecutionResponse> actions
) {
}
