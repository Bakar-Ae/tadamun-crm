package com.crm.backend.organization.workspace.dto;

import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;

import java.util.List;

public record WorkspaceResponse(
        Long organizationId,
        Long membershipId,
        String name,
        String slug,
        String timeZone,
        RoleName role,
        DataScope dataScope,
        List<PermissionName> permissions
) {
}
