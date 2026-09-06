package com.crm.backend.subscription.billing;

import com.crm.backend.subscription.SubscriptionPlanCode;

import java.time.LocalDateTime;

public record BillingWebhookNotification(
        String providerEventId,
        String providerEventType,
        BillingWebhookType type,
        Long organizationId,
        String providerCustomerId,
        String providerSubscriptionId,
        String providerPriceId,
        SubscriptionPlanCode planCode,
        BillingProviderSubscriptionStatus subscriptionStatus,
        LocalDateTime occurredAt,
        LocalDateTime periodStartsAt,
        LocalDateTime periodEndsAt,
        boolean cancelAtPeriodEnd
) {
}
