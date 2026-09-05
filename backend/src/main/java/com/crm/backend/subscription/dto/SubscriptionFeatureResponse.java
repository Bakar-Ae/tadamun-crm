package com.crm.backend.subscription.dto;

import com.crm.backend.subscription.SubscriptionFeature;

public record SubscriptionFeatureResponse(
        SubscriptionFeature feature,
        boolean enabled,
        Long limitValue
) {
}
