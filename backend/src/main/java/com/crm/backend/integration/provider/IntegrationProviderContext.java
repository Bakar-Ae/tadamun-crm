package com.crm.backend.integration.provider;

import java.util.Map;
import java.util.Objects;

public record IntegrationProviderContext(
        Long organizationId,
        String publicConnectionId,
        Map<String, Object> configuration,
        IntegrationSecrets secrets
) {

    public IntegrationProviderContext {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(
                publicConnectionId,
                "Public connection ID is required"
        );
        configuration = Map.copyOf(configuration);
        Objects.requireNonNull(secrets, "Integration secrets are required");
    }
}
