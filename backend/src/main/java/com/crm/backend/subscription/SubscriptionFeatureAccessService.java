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
        if (organizationId == null || feature == null) {
            throw new IllegalArgumentException(
                    "Organization and subscription feature are required"
            );
        }

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganizationId(organizationId)
                .orElseThrow(() ->
                        new SubscriptionFeatureUnavailableException(feature)
                );
        SubscriptionStatus effectiveStatus = lifecyclePolicy
                .resolveEffectiveStatus(subscription, timeProvider.now());
        boolean enabled = subscription.getPlan().getFeatures().stream()
                .anyMatch(planFeature ->
                        planFeature.getFeature() == feature
                                && planFeature.isEnabled()
                );

        if (!lifecyclePolicy.allowsAccess(effectiveStatus) || !enabled) {
            throw new SubscriptionFeatureUnavailableException(feature);
        }
    }
}
