package com.crm.backend.security;

import com.crm.backend.role.DataScope;

import java.util.Objects;

public record DataScopeContext(
        Long userId,
        Long teamId,
        DataScope scope
) {
    public DataScopeContext {
        Objects.requireNonNull(userId, "User ID is required");
        scope = scope == null ? DataScope.OWN : scope;
    }

    public boolean canAccess(
            Long ownerUserId,
            Long ownerTeamId
    ) {
        if (scope == DataScope.ALL) {
            return true;
        }

        if (ownerUserId == null) {
            return false;
        }

        if (scope == DataScope.OWN) {
            return userId.equals(ownerUserId);
        }

        if (teamId == null || ownerTeamId == null) {
            return userId.equals(ownerUserId);
        }

        return teamId.equals(ownerTeamId);
    }
}