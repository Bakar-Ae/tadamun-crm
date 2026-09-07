package com.crm.backend.webhook.dto;

import com.crm.backend.webhook.WebhookDeliveryStatus;

import java.time.LocalDateTime;

public record WebhookDeliveryResponse(
        String deliveryId,
        String eventId,
        String eventType,
        WebhookDeliveryStatus status,
        int attemptCount,
        LocalDateTime nextAttemptAt,
        Integer lastHttpStatus,
        Integer lastDurationMs,
        String lastErrorCategory,
        String lastError,
        LocalDateTime completedAt,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
}
