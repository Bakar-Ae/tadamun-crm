package com.crm.backend.integration.provider.http;

public record ProviderHttpResponse(
        int statusCode,
        String body
) {

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
