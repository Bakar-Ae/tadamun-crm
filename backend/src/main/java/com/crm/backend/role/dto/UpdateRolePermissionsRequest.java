package com.crm.backend.role.dto;

import com.crm.backend.permission.PermissionName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateRolePermissionsRequest(
        @NotNull(message = "Permission names are required")
        Set<@Valid @NotNull PermissionName> permissionNames
) {
}