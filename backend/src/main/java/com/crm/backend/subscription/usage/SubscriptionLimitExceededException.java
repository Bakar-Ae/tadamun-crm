package com.crm.backend.subscription.usage;

import com.crm.backend.subscription.SubscriptionFeature;

public class SubscriptionLimitExceededException extends RuntimeException {

    private final SubscriptionFeature feature;
    private final long used;
    private final long limit;

    public SubscriptionLimitExceededException(
            SubscriptionFeature feature,
            long used,
            long limit
    ) {
        super("Subscription limit reached for " + feature);
        this.feature = feature;
        this.used = used;
        this.limit = limit;
    }

    public SubscriptionFeature getFeature() {
        return feature;
    }

    public long getUsed() {
        return used;
    }

    public long getLimit() {
        return limit;
    }
}