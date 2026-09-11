package com.crm.backend.integration.delivery.dto;

import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.delivery.IntegrationDeliveryStatus;
import com.crm.backend.integration.provider.IntegrationDeliveryType;

import java.time.LocalDateTime;

public record IntegrationDeliveryResponse(
        String publicDeliveryId,
        Long connectionId,
        String connectionName,
        IntegrationProvider provider,
        IntegrationDeliveryType type,
        String destination,
        String subject,
        IntegrationDeliveryStatus status,
        int attemptCount,
        int maximumAttempts,
        LocalDateTime nextAttemptAt,
        String providerMessageId,
        String lastErrorCategory,
        String lastErrorMessage,
        Long createdByUserId,
        String createdByName,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
