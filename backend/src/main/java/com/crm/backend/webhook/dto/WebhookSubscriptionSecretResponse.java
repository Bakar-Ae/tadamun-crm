package com.crm.backend.webhook.dto;

public record WebhookSubscriptionSecretResponse(
        String signingSecret,
        WebhookSubscriptionResponse subscription
) {
}
