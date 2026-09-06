package com.crm.backend.subscription.billing;

public record BillingWebhookProcessingResult(
        boolean received,
        boolean duplicate,
        BillingWebhookProcessingStatus status
) {
}
