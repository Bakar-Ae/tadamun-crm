package com.crm.backend.security.tenant;

import com.crm.backend.permission.Permission;
import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.Role;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TenantPermissionPolicy {

    private static final Set<PermissionName> PLATFORM_ONLY_PERMISSIONS =
            EnumSet.of(PermissionName.PERMISSION_MANAGE);

    public Set<PermissionName> resolvePermissions(Role membershipRole) {
        if (membershipRole == null) {
            throw TenantAccessException.accessDenied();
        }

        return membershipRole.getPermissions().stream()
                .map(Permission::getName)
                .filter(permission ->
                        !PLATFORM_ONLY_PERMISSIONS.contains(permission)
                )
                .collect(Collectors.toUnmodifiableSet());
    }
}
