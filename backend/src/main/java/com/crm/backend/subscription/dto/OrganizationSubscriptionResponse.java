package com.crm.backend.subscription.dto;

import com.crm.backend.subscription.SubscriptionStatus;

import java.time.LocalDateTime;

public record OrganizationSubscriptionResponse(
        Long id,
        Long organizationId,
        SubscriptionPlanResponse plan,
        SubscriptionStatus status,
        boolean accessAllowed,
        LocalDateTime startedAt,
        LocalDateTime trialEndsAt,
        LocalDateTime currentPeriodStartsAt,
        LocalDateTime currentPeriodEndsAt,
        LocalDateTime gracePeriodEndsAt,
        boolean cancelAtPeriodEnd,
        LocalDateTime canceledAt,
        Long version
) {
}
