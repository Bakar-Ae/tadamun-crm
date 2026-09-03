package com.crm.backend.organization.invitation.dto;

import com.crm.backend.role.RoleName;

public record OrganizationInvitationAcceptanceResponse(
        Long organizationId,
        String organizationName,
        Long userId,
        String email,
        RoleName role
) {
}