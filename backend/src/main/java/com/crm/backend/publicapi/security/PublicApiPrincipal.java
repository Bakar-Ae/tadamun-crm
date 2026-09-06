package com.crm.backend.publicapi.security;

import com.crm.backend.publicapi.key.PublicApiScope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public record PublicApiPrincipal(
        Long apiKeyId,
        Long organizationId,
        String keyName,
        Set<PublicApiScope> scopes,
        int rateLimitPerMinute
) {

    public PublicApiPrincipal {
        Objects.requireNonNull(apiKeyId, "API key ID is required");
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(keyName, "API key name is required");
        scopes = Set.copyOf(Objects.requireNonNull(
                scopes,
                "API key scopes are required"
        ));

        if (rateLimitPerMinute < 1) {
            throw new IllegalArgumentException(
                    "API key rate limit must be positive"
            );
        }
    }

    public Collection<GrantedAuthority> authorities() {
        return scopes.stream()
                .map(scope -> new SimpleGrantedAuthority(
                        "SCOPE_" + scope.getValue()
                ))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
