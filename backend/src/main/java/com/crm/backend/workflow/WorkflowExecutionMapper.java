package com.crm.backend.workflow;

import com.crm.backend.workflow.dto.WorkflowActionExecutionResponse;
import com.crm.backend.workflow.dto.WorkflowExecutionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowExecutionMapper {

    public WorkflowExecutionResponse toResponse(
            WorkflowExecution execution,
            List<WorkflowActionExecution> actions
    ) {
        return new WorkflowExecutionResponse(
                execution.getPublicExecutionId(),
                execution.getWorkflow().getId(),
                execution.getWorkflow().getName(),
                execution.getWorkflowVersion(),
                execution.getTriggerEventType().getValue(),
                execution.getSourceEventPublicId(),
                execution.getCorrelationId(),
                execution.getStatus(),
                execution.getAttemptCount(),
                execution.getCurrentActionOrder(),
                execution.getLastErrorCategory(),
                execution.getLastError(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getCreatedAt(),
                actions.stream().map(this::toActionResponse).toList()
        );
    }

    private WorkflowActionExecutionResponse toActionResponse(
            WorkflowActionExecution actionExecution
    ) {
        return new WorkflowActionExecutionResponse(
                actionExecution.getPublicActionExecutionId(),
                actionExecution.getActionOrder(),
                actionExecution.getAction().getName(),
                actionExecution.getActionType(),
                actionExecution.getStatus(),
                actionExecution.getAttemptCount(),
                actionExecution.getResultResourceType(),
                actionExecution.getResultResourceId(),
                actionExecution.getLastErrorCategory(),
                actionExecution.getLastError(),
                actionExecution.getStartedAt(),
                actionExecution.getCompletedAt()
        );
    }
}
