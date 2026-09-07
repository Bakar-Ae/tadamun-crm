package com.crm.backend.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubscriptionFeatureAccessService {

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionLifecyclePolicy lifecyclePolicy;
    private final SubscriptionTimeProvider timeProvider;

    public SubscriptionFeatureAccessService(
            OrganizationSubscriptionRepository subscriptionRepository,
            SubscriptionLifecyclePolicy lifecyclePolicy,
            SubscriptionTimeProvider timeProvider
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.lifecyclePolicy = lifecyclePolicy;
        this.timeProvider = timeProvider;
    }

    public void requireFeature(
            Long organizationId,
            SubscriptionFeature feature
    ) {
        requireFeatureAndGetLimit(organizationId, feature);
    }

    public Long requireFeatureAndGetLimit(
            Long organizationId,
            SubscriptionFeature feature
    ) {
        return requireFeatureAndGetLimit(organizationId, feature, false);
    }

    public Long requireFeatureAndGetLimitForUpdate(
            Long organizationId,
            SubscriptionFeature feature
    ) {
        return requireFeatureAndGetLimit(organizationId, feature, true);
    }

    private Long requireFeatureAndGetLimit(
            Long organizationId,
            SubscriptionFeature feature,
            boolean lockForUpdate
    ) {
        if (organizationId == null || feature == null) {
            throw new IllegalArgumentException(
                    "Organization and subscription feature are required"
            );
        }

        OrganizationSubscription subscription = (lockForUpdate
                ? subscriptionRepository.findForUpdateByOrganizationId(
                        organizationId
                )
                : subscriptionRepository.findByOrganizationId(organizationId))
                .orElseThrow(() ->
                        new SubscriptionFeatureUnavailableException(feature)
                );
        SubscriptionStatus effectiveStatus = lifecyclePolicy
                .resolveEffectiveStatus(subscription, timeProvider.now());
        SubscriptionPlanFeature planFeature = subscription.getPlan()
                .getFeatures()
                .stream()
                .filter(item -> item.getFeature() == feature)
                .findFirst()
                .orElse(null);

        if (!lifecyclePolicy.allowsAccess(effectiveStatus)
                || planFeature == null
                || !planFeature.isEnabled()) {
            throw new SubscriptionFeatureUnavailableException(feature);
        }
        return planFeature.getLimitValue();
    }
}
