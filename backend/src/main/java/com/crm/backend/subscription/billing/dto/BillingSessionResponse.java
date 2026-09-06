package com.crm.backend.subscription.billing.dto;

public record BillingSessionResponse(
        String providerSessionId,
        String redirectUrl
) {
}
