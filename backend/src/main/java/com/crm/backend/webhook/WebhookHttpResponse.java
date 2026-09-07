package com.crm.backend.webhook;

public record WebhookHttpResponse(
        int statusCode,
        String responseExcerpt,
        String retryAfter
) {
}
