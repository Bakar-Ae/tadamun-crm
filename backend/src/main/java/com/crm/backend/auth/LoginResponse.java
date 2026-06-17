package com.crm.backend.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        String role,
        boolean passwordChangeRequired
) {
}