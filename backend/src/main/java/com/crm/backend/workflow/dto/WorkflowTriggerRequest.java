package com.crm.backend.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record WorkflowTriggerRequest(
        @NotBlank
        @Size(max = 100)
        String eventType,

        JsonNode conditionConfig,

        Boolean enabled
) {
}
