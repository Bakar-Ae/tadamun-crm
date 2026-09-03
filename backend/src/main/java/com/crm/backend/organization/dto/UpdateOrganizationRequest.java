package com.crm.backend.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 150, message = "Organization name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Organization time zone is required")
        @Size(max = 60, message = "Organization time zone must not exceed 60 characters")
        String timeZone,

        @NotNull(message = "Organization version is required")
        @PositiveOrZero(message = "Organization version is invalid")
        Long version
) {
}
