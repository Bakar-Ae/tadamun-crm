package com.crm.backend.subscription;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class SubscriptionLifecyclePolicy {

    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>>
            ALLOWED_TRANSITIONS = transitions();

    public SubscriptionStatus resolveEffectiveStatus(
            OrganizationSubscription subscription,
            LocalDateTime now
    ) {
        SubscriptionStatus status = subscription.getStatus();

        if (status == SubscriptionStatus.TRIALING
                && hasEnded(subscription.getTrialEndsAt(), now)) {
            return SubscriptionStatus.EXPIRED;
        }

        if (status == SubscriptionStatus.GRACE_PERIOD
                && hasEnded(subscription.getGracePeriodEndsAt(), now)) {
            return SubscriptionStatus.EXPIRED;
        }

        if (status == SubscriptionStatus.ACTIVE
                && subscription.isCancelAtPeriodEnd()
                && hasEnded(subscription.getCurrentPeriodEndsAt(), now)) {
            return SubscriptionStatus.CANCELED;
        }

        return status;
    }

    public boolean allowsAccess(SubscriptionStatus status) {
        return status == SubscriptionStatus.TRIALING
                || status == SubscriptionStatus.ACTIVE
                || status == SubscriptionStatus.PAST_DUE
                || status == SubscriptionStatus.GRACE_PERIOD;
    }

    public void requireAllowedTransition(
            SubscriptionStatus current,
            SubscriptionStatus next
    ) {
        if (current == next) {
            return;
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of())
                .contains(next)) {
            throw new IllegalArgumentException(
                    "Subscription cannot transition from "
                            + current + " to " + next
            );
        }
    }

    public LocalDateTime calculateTrialEnd(
            SubscriptionPlan plan,
            LocalDateTime startedAt
    ) {
        return startedAt.plusDays(plan.getTrialDays());
    }

    public LocalDateTime calculateGracePeriodEnd(
            SubscriptionPlan plan,
            LocalDateTime startedAt
    ) {
        return startedAt.plusDays(plan.getGracePeriodDays());
    }

    private boolean hasEnded(LocalDateTime end, LocalDateTime now) {
        return end != null && !now.isBefore(end);
    }

    private static Map<SubscriptionStatus, Set<SubscriptionStatus>>
    transitions() {
        EnumMap<SubscriptionStatus, Set<SubscriptionStatus>> transitions =
                new EnumMap<>(SubscriptionStatus.class);

        transitions.put(
                SubscriptionStatus.TRIALING,
                EnumSet.of(
                        SubscriptionStatus.ACTIVE,
                        SubscriptionStatus.CANCELED,
                        SubscriptionStatus.EXPIRED
                )
        );
        transitions.put(
                SubscriptionStatus.ACTIVE,
                EnumSet.of(
                        SubscriptionStatus.PAST_DUE,
                        SubscriptionStatus.GRACE_PERIOD,
                        SubscriptionStatus.CANCELED
                )
        );
        transitions.put(
                SubscriptionStatus.PAST_DUE,
                EnumSet.of(
                        SubscriptionStatus.ACTIVE,
                        SubscriptionStatus.GRACE_PERIOD,
                        SubscriptionStatus.CANCELED,
                        SubscriptionStatus.EXPIRED
                )
        );
        transitions.put(
                SubscriptionStatus.GRACE_PERIOD,
                EnumSet.of(
                        SubscriptionStatus.ACTIVE,
                        SubscriptionStatus.CANCELED,
                        SubscriptionStatus.EXPIRED
                )
        );
        transitions.put(
                SubscriptionStatus.CANCELED,
                EnumSet.of(SubscriptionStatus.ACTIVE)
        );
        transitions.put(
                SubscriptionStatus.EXPIRED,
                EnumSet.of(SubscriptionStatus.ACTIVE)
        );

        return Map.copyOf(transitions);
    }
}
