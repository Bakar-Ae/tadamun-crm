package com.crm.backend.auth;

import com.crm.backend.permission.PermissionName;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        String role,
        List<PermissionName> permissions,
        boolean passwordChangeRequired
) {
}
