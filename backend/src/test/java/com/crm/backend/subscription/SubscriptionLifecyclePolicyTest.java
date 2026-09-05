package com.crm.backend.subscription;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionLifecyclePolicyTest {

    private final SubscriptionLifecyclePolicy policy =
            new SubscriptionLifecyclePolicy();
    private final LocalDateTime now =
            LocalDateTime.of(2026, 9, 5, 10, 0);

    @Test
    void expiredTrialShouldResolveToExpired() {
        OrganizationSubscription subscription = subscription(
                SubscriptionStatus.TRIALING
        );
        subscription.setTrialEndsAt(now.minusSeconds(1));

        assertEquals(
                SubscriptionStatus.EXPIRED,
                policy.resolveEffectiveStatus(subscription, now)
        );
    }

    @Test
    void currentTrialShouldRemainTrialing() {
        OrganizationSubscription subscription = subscription(
                SubscriptionStatus.TRIALING
        );
        subscription.setTrialEndsAt(now.plusDays(1));

        assertEquals(
                SubscriptionStatus.TRIALING,
                policy.resolveEffectiveStatus(subscription, now)
        );
    }

    @Test
    void endedGracePeriodShouldResolveToExpired() {
        OrganizationSubscription subscription = subscription(
                SubscriptionStatus.GRACE_PERIOD
        );
        subscription.setGracePeriodEndsAt(now);

        assertEquals(
                SubscriptionStatus.EXPIRED,
                policy.resolveEffectiveStatus(subscription, now)
        );
    }

    @Test
    void scheduledCancellationShouldResolveAtPeriodEnd() {
        OrganizationSubscription subscription = subscription(
                SubscriptionStatus.ACTIVE
        );
        subscription.setCancelAtPeriodEnd(true);
        subscription.setCurrentPeriodEndsAt(now);

        assertEquals(
                SubscriptionStatus.CANCELED,
                policy.resolveEffectiveStatus(subscription, now)
        );
    }

    @Test
    void onlyUsableStatusesShouldAllowAccess() {
        assertTrue(policy.allowsAccess(SubscriptionStatus.TRIALING));
        assertTrue(policy.allowsAccess(SubscriptionStatus.ACTIVE));
        assertTrue(policy.allowsAccess(SubscriptionStatus.PAST_DUE));
        assertTrue(policy.allowsAccess(SubscriptionStatus.GRACE_PERIOD));
        assertFalse(policy.allowsAccess(SubscriptionStatus.CANCELED));
        assertFalse(policy.allowsAccess(SubscriptionStatus.EXPIRED));
    }

    @Test
    void invalidTransitionShouldBeRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> policy.requireAllowedTransition(
                        SubscriptionStatus.TRIALING,
                        SubscriptionStatus.GRACE_PERIOD
                )
        );

        assertEquals(
                "Subscription cannot transition from TRIALING to GRACE_PERIOD",
                exception.getMessage()
        );
    }

    @Test
    void planRulesShouldCalculateTrialAndGraceEnds() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setTrialDays(14);
        plan.setGracePeriodDays(7);

        assertEquals(now.plusDays(14), policy.calculateTrialEnd(plan, now));
        assertEquals(
                now.plusDays(7),
                policy.calculateGracePeriodEnd(plan, now)
        );
    }

    private OrganizationSubscription subscription(
            SubscriptionStatus status
    ) {
        OrganizationSubscription subscription =
                new OrganizationSubscription();
        subscription.setStatus(status);
        return subscription;
    }
}
