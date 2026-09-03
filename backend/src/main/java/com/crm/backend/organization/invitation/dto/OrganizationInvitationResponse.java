package com.crm.backend.organization.invitation.dto;

import com.crm.backend.organization.invitation.OrganizationInvitationStatus;
import com.crm.backend.role.RoleName;

import java.time.LocalDateTime;

public record OrganizationInvitationResponse(
        Long id,
        Long organizationId,
        String organizationName,
        String email,
        RoleName role,
        OrganizationInvitationStatus status,
        Long invitedByUserId,
        String invitedByUserName,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt
) {
}
