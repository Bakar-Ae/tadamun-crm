package com.crm.backend.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateWebhookSubscriptionRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 2048)
        String endpointUrl,

        @NotEmpty
        @Size(max = 14)
        Set<@NotBlank String> eventTypes
) {
}
