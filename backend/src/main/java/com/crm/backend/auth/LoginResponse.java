package com.crm.backend.auth;

public record LoginResponse(
        String token,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        String role
) {
}