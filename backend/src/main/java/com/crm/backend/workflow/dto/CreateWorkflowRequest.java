package com.crm.backend.workflow.dto;

import com.crm.backend.workflow.WorkflowFailurePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateWorkflowRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        WorkflowFailurePolicy failurePolicy,

        @Min(5)
        @Max(300)
        Integer executionTimeoutSeconds,

        @Min(1)
        @Max(5)
        Integer maximumAttempts,

        @Min(1)
        @Max(10000)
        Integer maximumExecutionsPerHour,

        @NotNull
        @Valid
        WorkflowTriggerRequest trigger,

        @NotEmpty
        @Size(max = 10)
        List<@Valid WorkflowActionRequest> actions
) {
}
