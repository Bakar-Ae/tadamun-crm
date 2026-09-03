package com.crm.backend.organization.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptOrganizationInvitationRequest(
        @NotBlank(message = "Invitation token is required")
        @Size(max = 512, message = "Invitation token is invalid")
        String token,

        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Size(min = 8, max = 100,
                message = "Password must be between 8 and 100 characters")
        String password,

        @Size(min = 8, max = 100,
                message = "Password confirmation must be between 8 and 100 characters")
        String confirmPassword
) {
}