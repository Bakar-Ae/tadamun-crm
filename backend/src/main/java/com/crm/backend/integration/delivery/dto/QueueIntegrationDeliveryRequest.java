package com.crm.backend.integration.delivery.dto;

import com.crm.backend.integration.provider.IntegrationDeliveryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QueueIntegrationDeliveryRequest(
        @NotNull IntegrationDeliveryType type,
        @NotBlank @Size(max = 320) String destination,
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 50_000) String body,
        @Size(max = 100) String idempotencyKey
) {
}
