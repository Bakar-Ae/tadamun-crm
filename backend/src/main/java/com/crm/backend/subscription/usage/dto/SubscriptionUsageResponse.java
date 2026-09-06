package com.crm.backend.subscription.usage.dto;

import com.crm.backend.subscription.SubscriptionPlanCode;

import java.util.List;

public record SubscriptionUsageResponse(
        Long organizationId,
        SubscriptionPlanCode planCode,
        List<SubscriptionUsageMetricResponse> metrics
) {
}