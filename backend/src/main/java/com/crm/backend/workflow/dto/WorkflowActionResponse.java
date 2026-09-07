package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowActionType;
import tools.jackson.databind.JsonNode;

public record WorkflowActionResponse(
        Long id,
        String publicActionId,
        int actionOrder,
        String name,
        WorkflowActionType actionType,
        JsonNode configuration,
        boolean enabled,
        int timeoutSeconds
) {
}
