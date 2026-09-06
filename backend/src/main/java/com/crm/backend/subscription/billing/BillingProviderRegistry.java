package com.crm.backend.subscription.billing;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BillingProviderRegistry {

    private final Map<BillingProviderName, BillingProvider> providers;

    public BillingProviderRegistry(List<BillingProvider> providers) {
        EnumMap<BillingProviderName, BillingProvider> configured =
                new EnumMap<>(BillingProviderName.class);

        for (BillingProvider provider : providers) {
            BillingProvider previous = configured.put(
                    provider.name(),
                    provider
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple billing providers configured for "
                                + provider.name()
                );
            }
        }

        this.providers = Map.copyOf(configured);
    }

    public BillingProvider require(BillingProviderName name) {
        BillingProvider provider = providers.get(name);
        if (provider == null) {
            throw new BillingUnavailableException(
                    "Billing is currently unavailable"
            );
        }
        return provider;
    }
}
