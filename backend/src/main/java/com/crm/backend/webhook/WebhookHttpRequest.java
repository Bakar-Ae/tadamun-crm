package com.crm.backend.webhook;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public record WebhookHttpRequest(
        URI endpoint,
        byte[] body,
        Map<String, String> headers
) {
    public WebhookHttpRequest {
        Objects.requireNonNull(endpoint, "Webhook endpoint is required");
        Objects.requireNonNull(body, "Webhook body is required");
        Objects.requireNonNull(headers, "Webhook headers are required");
        body = body.clone();
        headers = Map.copyOf(headers);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
