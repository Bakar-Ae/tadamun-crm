package com.crm.backend.subscription;

public class SubscriptionFeatureUnavailableException
        extends RuntimeException {

    private final SubscriptionFeature feature;

    public SubscriptionFeatureUnavailableException(
            SubscriptionFeature feature
    ) {
        super("Current subscription does not include " + feature);
        this.feature = feature;
    }

    public SubscriptionFeature getFeature() {
        return feature;
    }
}
