package com.crm.backend.organization.membership.dto;

import com.crm.backend.role.RoleName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateOrganizationMembershipRequest(
        @NotNull(message = "Organization role is required")
        RoleName role,

        @NotNull(message = "Membership version is required")
        @PositiveOrZero(message = "Membership version is invalid")
        Long version
) {
}
