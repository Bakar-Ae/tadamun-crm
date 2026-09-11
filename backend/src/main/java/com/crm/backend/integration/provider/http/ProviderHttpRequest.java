package com.crm.backend.integration.provider.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ProviderHttpRequest(
        String method,
        URI uri,
        Map<String, String> headers,
        String body,
        Duration timeout
) {

    public ProviderHttpRequest {
        Objects.requireNonNull(method, "HTTP method is required");
        Objects.requireNonNull(uri, "HTTP URI is required");
        Objects.requireNonNull(headers, "HTTP headers are required");
        Objects.requireNonNull(timeout, "HTTP timeout is required");
        headers = Map.copyOf(headers);
    }

    @Override
    public String toString() {
        return "ProviderHttpRequest{method=" + method
                + ", uri=" + uri
                + ", headers=<redacted>, bodyLength="
                + (body == null ? 0 : body.length()) + '}';
    }
}
