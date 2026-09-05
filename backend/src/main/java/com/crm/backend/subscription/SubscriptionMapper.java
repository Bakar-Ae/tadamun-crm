package com.crm.backend.subscription;

import com.crm.backend.subscription.dto.OrganizationSubscriptionResponse;
import com.crm.backend.subscription.dto.SubscriptionFeatureResponse;
import com.crm.backend.subscription.dto.SubscriptionPlanResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class SubscriptionMapper {

    public SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getTrialDays(),
                plan.getGracePeriodDays(),
                plan.getFeatures().stream()
                        .sorted(Comparator.comparing(feature ->
                                feature.getFeature().name()))
                        .map(this::toFeatureResponse)
                        .toList()
        );
    }

    public OrganizationSubscriptionResponse toResponse(
            OrganizationSubscription subscription,
            SubscriptionStatus effectiveStatus,
            boolean accessAllowed
    ) {
        return new OrganizationSubscriptionResponse(
                subscription.getId(),
                subscription.getOrganization().getId(),
                toPlanResponse(subscription.getPlan()),
                effectiveStatus,
                accessAllowed,
                subscription.getStartedAt(),
                subscription.getTrialEndsAt(),
                subscription.getCurrentPeriodStartsAt(),
                subscription.getCurrentPeriodEndsAt(),
                subscription.getGracePeriodEndsAt(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCanceledAt(),
                subscription.getVersion()
        );
    }

    private SubscriptionFeatureResponse toFeatureResponse(
            SubscriptionPlanFeature feature
    ) {
        return new SubscriptionFeatureResponse(
                feature.getFeature(),
                feature.isEnabled(),
                feature.getLimitValue()
        );
    }
}
