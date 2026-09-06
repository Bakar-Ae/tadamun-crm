package com.crm.backend.subscription.billing.dto;

import com.crm.backend.subscription.billing.BillingWebhookProcessingStatus;

public record BillingWebhookResponse(
        boolean received,
        boolean duplicate,
        BillingWebhookProcessingStatus status
) {
}
