package com.crm.backend.publicapi.key;

import java.util.Arrays;

public enum PublicApiScope {
    CUSTOMERS_READ("customers:read"),
    LEADS_READ("leads:read");

    private final String value;

    PublicApiScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PublicApiScope fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Public API scope is required");
        }

        return Arrays.stream(values())
                .filter(scope -> scope.value.equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown public API scope: " + value
                ));
    }
}
