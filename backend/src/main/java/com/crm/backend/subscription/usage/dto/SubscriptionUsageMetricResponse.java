package com.crm.backend.subscription.usage.dto;

import com.crm.backend.subscription.SubscriptionFeature;

public record SubscriptionUsageMetricResponse(
        SubscriptionFeature feature,
        long used,
        Long limit,
        Long remaining,
        Integer percentageUsed,
        boolean warning,
        boolean limitReached
) {
}