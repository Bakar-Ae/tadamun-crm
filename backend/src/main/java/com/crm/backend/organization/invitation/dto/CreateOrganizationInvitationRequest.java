package com.crm.backend.organization.invitation.dto;

import com.crm.backend.role.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrganizationInvitationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 190, message = "Email must not exceed 190 characters")
        String email,

        @NotNull(message = "Organization role is required")
        RoleName role
) {
}
