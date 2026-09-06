package com.crm.backend.subscription.billing.dto;

import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.billing.BillingInterval;
import jakarta.validation.constraints.NotNull;

public record CreateBillingCheckoutRequest(
        @NotNull SubscriptionPlanCode planCode,
        @NotNull BillingInterval billingInterval
) {
}
