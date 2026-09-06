package com.crm.backend.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionFeatureAccessServiceTest {

    private OrganizationSubscriptionRepository subscriptionRepository;
    private SubscriptionTimeProvider timeProvider;
    private SubscriptionFeatureAccessService accessService;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(OrganizationSubscriptionRepository.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        accessService = new SubscriptionFeatureAccessService(
                subscriptionRepository,
                new SubscriptionLifecyclePolicy(),
                timeProvider
        );
        when(timeProvider.now()).thenReturn(
                LocalDateTime.of(2026, 9, 6, 12, 0)
        );
    }

    @Test
    void enabledFeatureOnActiveSubscriptionShouldBeAllowed() {
        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription(true)));

        assertDoesNotThrow(() -> accessService.requireFeature(
                10L,
                SubscriptionFeature.PUBLIC_API
        ));
    }

    @Test
    void disabledFeatureShouldRequireUpgrade() {
        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription(false)));

        assertThrows(
                SubscriptionFeatureUnavailableException.class,
                () -> accessService.requireFeature(
                        10L,
                        SubscriptionFeature.PUBLIC_API
                )
        );
    }

    private OrganizationSubscription subscription(boolean enabled) {
        SubscriptionPlan plan = new SubscriptionPlan();
        SubscriptionPlanFeature planFeature = new SubscriptionPlanFeature();
        planFeature.setPlan(plan);
        planFeature.setFeature(SubscriptionFeature.PUBLIC_API);
        planFeature.setEnabled(enabled);
        plan.setFeatures(List.of(planFeature));

        OrganizationSubscription subscription =
                new OrganizationSubscription();
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }
}
