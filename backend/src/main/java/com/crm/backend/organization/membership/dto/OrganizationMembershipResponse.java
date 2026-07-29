package com.crm.backend.organization.membership.dto;

import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.RoleName;
import com.crm.backend.user.UserStatus;

import java.time.LocalDateTime;

public record OrganizationMembershipResponse(
        Long id,
        Long organizationId,
        String organizationName,
        String organizationSlug,
        OrganizationStatus organizationStatus,
        Long userId,
        String userFullName,
        String userEmail,
        UserStatus userStatus,
        RoleName role,
        OrganizationMembershipStatus status,
        LocalDateTime joinedAt,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}