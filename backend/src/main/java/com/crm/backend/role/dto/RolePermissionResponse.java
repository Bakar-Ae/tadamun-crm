package com.crm.backend.role.dto;

import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.RoleName;

import java.util.List;

public record RolePermissionResponse(
        RoleName name,
        String description,
        boolean editable,
        List<PermissionName> permissions
) {
}