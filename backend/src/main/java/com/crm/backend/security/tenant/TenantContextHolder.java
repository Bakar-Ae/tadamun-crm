package com.crm.backend.security.tenant;

import java.util.Optional;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT =
            new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Tenant context must not be null"
            );
        }

        CONTEXT.set(context);
    }

    public static Optional<TenantContext> getOptional() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static TenantContext getRequired() {
        return getOptional().orElseThrow(() ->
                new IllegalStateException(
                        "Tenant context is required for this operation"
                ));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
