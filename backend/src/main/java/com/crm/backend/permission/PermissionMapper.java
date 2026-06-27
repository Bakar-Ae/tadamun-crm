package com.crm.backend.permission;

import com.crm.backend.permission.dto.PermissionResponse;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getName(),
                permission.getDescription()
        );
    }
}