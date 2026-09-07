package com.crm.backend.webhook.dto;

import com.crm.backend.webhook.WebhookSubscriptionStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record WebhookSubscriptionResponse(
        Long id,
        String name,
        String endpointUrl,
        WebhookSubscriptionStatus status,
        String secretDisplaySuffix,
        Set<String> eventTypes,
        int consecutiveFailures,
        LocalDateTime lastSuccessAt,
        LocalDateTime lastFailureAt,
        Long createdByUserId,
        String createdByName,
        Long updatedByUserId,
        String updatedByName,
        Long revokedByUserId,
        String revokedByName,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
