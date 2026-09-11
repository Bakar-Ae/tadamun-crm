package com.crm.backend.integration.security;

import com.crm.backend.integration.IntegrationProvider;

import java.util.Objects;

public record IntegrationCredentialContext(
        Long organizationId,
        String publicConnectionId,
        IntegrationProvider provider
) {

    public IntegrationCredentialContext {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(
                publicConnectionId,
                "Public connection ID is required"
        );
        Objects.requireNonNull(provider, "Integration provider is required");
        if (organizationId <= 0) {
            throw new IllegalArgumentException(
                    "Organization ID must be positive"
            );
        }
        if (publicConnectionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Public connection ID is required"
            );
        }
    }
}
