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
