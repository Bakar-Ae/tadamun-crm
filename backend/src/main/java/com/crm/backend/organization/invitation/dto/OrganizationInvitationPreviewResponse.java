package com.crm.backend.organization.invitation.dto;

import com.crm.backend.role.RoleName;

import java.time.LocalDateTime;

public record OrganizationInvitationPreviewResponse(
        String organizationName,
        String email,
        RoleName role,
        LocalDateTime expiresAt,
        boolean requiresAccountCreation
) {
}