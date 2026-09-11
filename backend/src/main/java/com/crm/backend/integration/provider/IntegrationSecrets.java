package com.crm.backend.integration.provider;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class IntegrationSecrets {

    private final Map<String, String> values;

    public IntegrationSecrets(Map<String, String> values) {
        Objects.requireNonNull(values, "Secret values are required");
        this.values = Map.copyOf(values);
    }

    public String require(String name) {
        return optional(name).orElseThrow(() -> new IllegalArgumentException(
                "Missing integration credential: " + name
        ));
    }

    public Optional<String> optional(String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public Set<String> names() {
        return values.keySet();
    }

    @Override
    public String toString() {
        return "IntegrationSecrets{names=" + names() + ", values=<redacted>}";
    }
}
