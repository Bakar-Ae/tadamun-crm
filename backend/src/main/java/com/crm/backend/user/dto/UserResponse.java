package com.crm.backend.user.dto;

import com.crm.backend.role.RoleName;
import com.crm.backend.user.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        RoleName role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}