package com.crm.backend.integration.provider;

import java.util.Objects;

public record IntegrationOutboundMessage(
        IntegrationDeliveryType type,
        String destination,
        String subject,
        String body,
        String idempotencyKey
) {

    public IntegrationOutboundMessage {
        Objects.requireNonNull(type, "Delivery type is required");
        Objects.requireNonNull(destination, "Destination is required");
        Objects.requireNonNull(body, "Message body is required");
        Objects.requireNonNull(idempotencyKey, "Idempotency key is required");
    }
}
