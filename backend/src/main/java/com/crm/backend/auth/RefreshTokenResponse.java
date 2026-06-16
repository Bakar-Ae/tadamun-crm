package com.crm.backend.auth;

public record RefreshTokenResponse(
        String accessToken,
        String tokenType
) {
}