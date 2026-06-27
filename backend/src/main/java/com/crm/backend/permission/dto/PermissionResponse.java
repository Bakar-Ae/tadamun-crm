package com.crm.backend.permission.dto;

import com.crm.backend.permission.PermissionName;

public record PermissionResponse(
        PermissionName name,
        String description
) {
}