package com.crm.backend.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Organization slug is required")
        @Size(max = 100)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must contain lowercase letters, numbers, and single hyphens"
        )
        String slug,

        @NotBlank(message = "Organization time zone is required")
        @Size(max = 60)
        String timeZone
) {
}
