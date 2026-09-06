package com.crm.backend.publicapi.security;

import java.util.Optional;

public final class PublicApiContextHolder {

    private static final ThreadLocal<PublicApiPrincipal> CONTEXT =
            new ThreadLocal<>();

    private PublicApiContextHolder() {
    }

    public static void set(PublicApiPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException(
                    "Public API context must not be null"
            );
        }
        CONTEXT.set(principal);
    }

    public static Optional<PublicApiPrincipal> getOptional() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static PublicApiPrincipal getRequired() {
        return getOptional().orElseThrow(() ->
                new IllegalStateException(
                        "Public API context is required for this operation"
                )
        );
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
