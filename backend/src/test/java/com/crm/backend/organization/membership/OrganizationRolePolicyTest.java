package com.crm.backend.organization.membership;

import com.crm.backend.role.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationRolePolicyTest {

    private final OrganizationRolePolicy policy =
            new OrganizationRolePolicy();

    @Test
    void ownerShouldBeAllowedToAssignAdministrator() {
        assertDoesNotThrow(() ->
                policy.requireCanAssign(RoleName.OWNER, RoleName.ADMIN)
        );
    }

    @Test
    void administratorShouldBeAllowedToAssignOperationalRoles() {
        assertDoesNotThrow(() ->
                policy.requireCanAssign(
                        RoleName.ADMIN,
                        RoleName.SALES_REP
                )
        );
    }

    @Test
    void administratorShouldNotAssignAnotherAdministrator() {
        assertThrows(
                AccessDeniedException.class,
                () -> policy.requireCanAssign(
                        RoleName.ADMIN,
                        RoleName.ADMIN
                )
        );
    }

    @Test
    void ownerRoleShouldRequireDedicatedOwnershipTransfer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.requireCanAssign(
                        RoleName.OWNER,
                        RoleName.OWNER
                )
        );
    }

    @Test
    void nonAdministratorShouldNotAssignRoles() {
        assertThrows(
                AccessDeniedException.class,
                () -> policy.requireCanAssign(
                        RoleName.MANAGER,
                        RoleName.SALES_REP
                )
        );
    }
}
