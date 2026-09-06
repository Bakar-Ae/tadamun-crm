package com.crm.backend.publicapi.key.dto;

import com.crm.backend.publicapi.key.PublicApiKeyStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record PublicApiKeyResponse(
        Long id,
        String name,
        String displayPrefix,
        PublicApiKeyStatus status,
        Set<String> scopes,
        Integer rateLimitPerMinute,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        Long createdByUserId,
        String createdByName,
        Long revokedByUserId,
        String revokedByName,
        LocalDateTime revokedAt,
        LocalDateTime createdAt
) {
}
