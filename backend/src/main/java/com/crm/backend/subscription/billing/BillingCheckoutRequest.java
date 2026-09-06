package com.crm.backend.subscription.billing;

import com.crm.backend.subscription.SubscriptionPlanCode;

import java.net.URI;

public record BillingCheckoutRequest(
        Long organizationId,
        String organizationName,
        String customerEmail,
        SubscriptionPlanCode planCode,
        String providerCustomerId,
        String providerPriceId,
        URI successUrl,
        URI cancelUrl,
        String idempotencyKey
) {
}
