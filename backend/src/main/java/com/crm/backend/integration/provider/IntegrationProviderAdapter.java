package com.crm.backend.integration.provider;

import com.crm.backend.integration.IntegrationProvider;

import java.util.Map;
import java.util.Set;

public interface IntegrationProviderAdapter {

    IntegrationProvider provider();

    Set<IntegrationCapability> capabilities();

    void validateConfiguration(Map<String, Object> configuration);

    void validateCredentials(IntegrationSecrets secrets);

    IntegrationVerificationResult verify(IntegrationProviderContext context);

    IntegrationProviderDeliveryResult deliver(
            IntegrationProviderContext context,
            IntegrationOutboundMessage message
    );
}
