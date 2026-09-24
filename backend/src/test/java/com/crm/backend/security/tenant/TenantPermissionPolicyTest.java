package com.crm.backend.security.tenant;

import com.crm.backend.permission.Permission;
import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantPermissionPolicyTest {

    @Test
    void globalAccountMutationPermissionsShouldNotEnterTenantAuthorities() {
        Role membershipRole = new Role();
        membershipRole.setName(RoleName.OWNER);
        membershipRole.setPermissions(Set.of(
                permission(PermissionName.USER_VIEW),
                permission(PermissionName.USER_CREATE),
                permission(PermissionName.USER_UPDATE),
                permission(PermissionName.USER_DEACTIVATE),
                permission(PermissionName.USER_ROLE_CHANGE),
                permission(PermissionName.MEMBERSHIP_INVITE),
                permission(PermissionName.MEMBERSHIP_UPDATE),
                permission(PermissionName.MEMBERSHIP_DEACTIVATE)
        ));

        assertEquals(Set.of(
                PermissionName.USER_VIEW,
                PermissionName.MEMBERSHIP_INVITE,
                PermissionName.MEMBERSHIP_UPDATE,
                PermissionName.MEMBERSHIP_DEACTIVATE
        ), new TenantPermissionPolicy().resolvePermissions(membershipRole));
    }

    @Test
    void platformPermissionShouldNotEnterTenantAuthorities() {
        Permission customerView = permission(PermissionName.CUSTOMER_VIEW);
        Permission permissionManage = permission(
                PermissionName.PERMISSION_MANAGE
        );

        Role membershipRole = new Role();
        membershipRole.setName(RoleName.ADMIN);
        membershipRole.setPermissions(Set.of(
                customerView,
                permissionManage
        ));

        Set<PermissionName> permissions =
                new TenantPermissionPolicy()
                        .resolvePermissions(membershipRole);

        assertEquals(Set.of(PermissionName.CUSTOMER_VIEW), permissions);
    }

    private Permission permission(PermissionName name) {
        Permission permission = new Permission();
        permission.setName(name);
        return permission;
    }
}
