package com.crm.backend.subscription.dto;

import com.crm.backend.subscription.SubscriptionPlanCode;

import java.util.List;

public record SubscriptionPlanResponse(
        Long id,
        SubscriptionPlanCode code,
        String name,
        String description,
        int trialDays,
        int gracePeriodDays,
        List<SubscriptionFeatureResponse> features
) {
}
