package com.crm.backend.organization.membership;

import com.crm.backend.role.RoleName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class OrganizationRolePolicy {

    private static final Set<RoleName> ADMINISTRATOR_ROLES =
            EnumSet.of(RoleName.OWNER, RoleName.ADMIN);

    private static final Set<RoleName> INVITABLE_ROLES =
            EnumSet.of(
                    RoleName.ADMIN,
                    RoleName.MANAGER,
                    RoleName.SALES_REP,
                    RoleName.SUPPORT_STAFF
            );

    public boolean isAdministrator(RoleName roleName) {
        return roleName != null
                && ADMINISTRATOR_ROLES.contains(roleName);
    }

    public void requireCanAssign(
            RoleName actorRole,
            RoleName targetRole
    ) {
        if (!isAdministrator(actorRole)) {
            throw new AccessDeniedException(
                    "Organization administrator access is required"
            );
        }

        if (targetRole == null || !INVITABLE_ROLES.contains(targetRole)) {
            throw new IllegalArgumentException(
                    "This organization role cannot be assigned"
            );
        }

        if (targetRole == RoleName.ADMIN
                && actorRole != RoleName.OWNER) {
            throw new AccessDeniedException(
                    "Only the organization owner can assign administrators"
            );
        }
    }

    public void requireCanManage(
            RoleName actorRole,
            RoleName targetRole
    ) {
        if (!isAdministrator(actorRole)) {
            throw new AccessDeniedException(
                    "Organization administrator access is required"
            );
        }

        if (targetRole == RoleName.OWNER) {
            throw new AccessDeniedException(
                    "The organization owner cannot be managed"
            );
        }

        if (targetRole == RoleName.ADMIN
                && actorRole != RoleName.OWNER) {
            throw new AccessDeniedException(
                    "Only the organization owner can manage administrators"
            );
        }
    }
}
