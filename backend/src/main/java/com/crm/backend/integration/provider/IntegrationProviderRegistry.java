package com.crm.backend.integration.provider;

import com.crm.backend.integration.IntegrationProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class IntegrationProviderRegistry {

    private final Map<IntegrationProvider, IntegrationProviderAdapter> adapters;

    public IntegrationProviderRegistry(
            List<IntegrationProviderAdapter> configuredAdapters
    ) {
        EnumMap<IntegrationProvider, IntegrationProviderAdapter> registered =
                new EnumMap<>(IntegrationProvider.class);
        for (IntegrationProviderAdapter adapter : configuredAdapters) {
            if (registered.putIfAbsent(adapter.provider(), adapter) != null) {
                throw new IllegalStateException(
                        "Duplicate integration adapter: " + adapter.provider()
                );
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    public IntegrationProviderAdapter require(IntegrationProvider provider) {
        IntegrationProviderAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new IllegalStateException(
                    "Integration provider is not configured: " + provider
            );
        }
        return adapter;
    }

    public boolean supports(IntegrationProvider provider) {
        return adapters.containsKey(provider);
    }
}
