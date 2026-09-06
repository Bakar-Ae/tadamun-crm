package com.crm.backend.subscription.billing;

public record BillingCustomerRequest(
        Long organizationId,
        String organizationName,
        String customerEmail,
        String idempotencyKey
) {
}
