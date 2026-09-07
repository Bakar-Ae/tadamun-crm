package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowTriggerType;
import tools.jackson.databind.JsonNode;

public record WorkflowTriggerResponse(
        Long id,
        WorkflowTriggerType triggerType,
        String eventType,
        JsonNode conditionConfig,
        boolean enabled
) {
}
