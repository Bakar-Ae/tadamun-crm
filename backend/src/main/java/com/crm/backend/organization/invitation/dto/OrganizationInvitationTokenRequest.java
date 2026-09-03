package com.crm.backend.organization.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationInvitationTokenRequest(
        @NotBlank(message = "Invitation token is required")
        @Size(max = 512, message = "Invitation token is invalid")
        String token
) {
}