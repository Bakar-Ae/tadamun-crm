package com.crm.backend.organization.membership.dto;

import com.crm.backend.role.RoleName;
import jakarta.validation.constraints.NotNull;

public record CreateOrganizationMembershipRequest(
        @NotNull(message = "User is required")
        Long userId,

        @NotNull(message = "Organization role is required")
        RoleName role
) {
}