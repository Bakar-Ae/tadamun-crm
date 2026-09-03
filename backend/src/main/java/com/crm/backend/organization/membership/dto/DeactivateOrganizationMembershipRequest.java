package com.crm.backend.organization.membership.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DeactivateOrganizationMembershipRequest(
        @NotNull(message = "Membership version is required")
        @PositiveOrZero(message = "Membership version is invalid")
        Long version
) {
}
