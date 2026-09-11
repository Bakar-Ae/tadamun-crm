package com.crm.backend.integration.dto;

import com.crm.backend.integration.IntegrationConnectionStatus;
import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.provider.IntegrationCapability;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public record IntegrationConnectionResponse(
        Long id,
        String publicConnectionId,
        IntegrationProvider provider,
        String name,
        IntegrationConnectionStatus status,
        Map<String, Object> configuration,
        Set<IntegrationCapability> capabilities,
        boolean credentialsConfigured,
        String externalAccountId,
        LocalDateTime credentialsUpdatedAt,
        LocalDateTime lastVerifiedAt,
        String lastErrorCategory,
        String lastErrorMessage,
        Long createdByUserId,
        String createdByName,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
