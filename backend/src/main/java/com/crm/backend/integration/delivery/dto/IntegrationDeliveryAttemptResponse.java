package com.crm.backend.integration.delivery.dto;

import com.crm.backend.integration.delivery.IntegrationDeliveryOutcome;

import java.time.LocalDateTime;

public record IntegrationDeliveryAttemptResponse(
        int attemptNumber,
        IntegrationDeliveryOutcome outcome,
        int durationMs,
        String providerMessageId,
        String errorCategory,
        String errorMessage,
        LocalDateTime attemptedAt
) {
}
