package com.crm.backend.webhook;

import java.util.Objects;

public record WebhookWorkItem(Long id, String claimToken) {

    public WebhookWorkItem {
        Objects.requireNonNull(id, "Webhook work item ID is required");
        if (claimToken == null || claimToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook claim token is required"
            );
        }
    }
}
