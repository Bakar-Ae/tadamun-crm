package com.crm.backend.webhook.dto;

import com.crm.backend.webhook.WebhookDeliveryOutcome;

import java.time.LocalDateTime;

public record WebhookDeliveryAttemptResponse(
        int attemptNumber,
        WebhookDeliveryOutcome outcome,
        long requestTimestamp,
        Integer httpStatus,
        int durationMs,
        String responseExcerpt,
        String errorCategory,
        String errorMessage,
        LocalDateTime attemptedAt
) {
}
