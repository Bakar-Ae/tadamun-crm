package com.crm.backend.security;

import com.crm.backend.role.DataScope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DataScopeService {

    public DataScopeContext currentContext() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {
            throw new AccessDeniedException(
                    "Authenticated user details are unavailable"
            );
        }

        DataScope scope = userDetails.getDataScope() == null
                ? DataScope.OWN
                : userDetails.getDataScope();

        return new DataScopeContext(
                userDetails.getId(),
                userDetails.getTeamId(),
                scope
        );
    }

    public void requireAccess(
            DataScopeContext context,
            Long ownerUserId,
            Long ownerTeamId
    ) {
        if (!context.canAccess(ownerUserId, ownerTeamId)) {
            throw new AccessDeniedException(
                    "You do not have access to this record"
            );
        }
    }

    public void requireAccess(
            Long ownerUserId,
            Long ownerTeamId
    ) {
        requireAccess(currentContext(), ownerUserId, ownerTeamId);
    }
}