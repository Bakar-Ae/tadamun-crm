package com.crm.backend.security.tenant;

import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public record TenantContext(
        Long organizationId,
        Long membershipId,
        Long userId,
        RoleName roleName,
        DataScope dataScope,
        Long teamId,
        Set<PermissionName> permissions
) {

    public TenantContext {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(membershipId, "Membership ID is required");
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(roleName, "Role is required");
        Objects.requireNonNull(dataScope, "Data scope is required");
        permissions = Set.copyOf(Objects.requireNonNull(
                permissions,
                "Permissions are required"
        ));
    }

    public Collection<GrantedAuthority> authorities() {
        ArrayList<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(
                "ROLE_" + roleName.name()
        ));

        permissions.stream()
                .map(PermissionName::name)
                .sorted()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }
}
