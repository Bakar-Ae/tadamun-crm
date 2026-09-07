package com.crm.backend.workflow;

import com.crm.backend.user.User;
import com.crm.backend.workflow.dto.WorkflowActionResponse;
import com.crm.backend.workflow.dto.WorkflowResponse;
import com.crm.backend.workflow.dto.WorkflowTriggerResponse;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class WorkflowMapper {

    private final ObjectMapper objectMapper;

    public WorkflowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorkflowResponse toResponse(
            WorkflowDefinition workflow,
            WorkflowTrigger trigger,
            List<WorkflowAction> actions
    ) {
        User createdBy = workflow.getCreatedByUser();
        User updatedBy = workflow.getUpdatedByUser();
        WorkflowTriggerResponse triggerResponse = trigger == null
                ? null
                : new WorkflowTriggerResponse(
                        trigger.getId(),
                        trigger.getTriggerType(),
                        trigger.getEventType().getValue(),
                        readJson(trigger.getConditionConfig()),
                        trigger.isEnabled()
                );
        List<WorkflowActionResponse> actionResponses = actions.stream()
                .map(action -> new WorkflowActionResponse(
                        action.getId(),
                        action.getPublicActionId(),
                        action.getActionOrder(),
                        action.getName(),
                        action.getActionType(),
                        readJson(action.getConfiguration()),
                        action.isEnabled(),
                        action.getTimeoutSeconds()
                ))
                .toList();

        return new WorkflowResponse(
                workflow.getId(),
                workflow.getPublicWorkflowId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                workflow.getFailurePolicy(),
                workflow.getExecutionTimeoutSeconds(),
                workflow.getMaximumAttempts(),
                workflow.getMaximumExecutionsPerHour(),
                workflow.getDefinitionVersion(),
                triggerResponse,
                actionResponses,
                createdBy.getId(),
                createdBy.getFullName(),
                updatedBy == null ? null : updatedBy.getId(),
                updatedBy == null ? null : updatedBy.getFullName(),
                workflow.getActivatedAt(),
                workflow.getArchivedAt(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }

    private JsonNode readJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Stored workflow configuration is invalid"
            );
        }
    }
}
