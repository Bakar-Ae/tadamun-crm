package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowActionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record WorkflowActionRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        WorkflowActionType actionType,

        @NotNull
        JsonNode configuration,

        Boolean enabled,

        @Min(1)
        @Max(60)
        Integer timeoutSeconds
) {
}
