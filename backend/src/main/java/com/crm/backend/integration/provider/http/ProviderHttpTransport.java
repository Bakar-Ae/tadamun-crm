package com.crm.backend.integration.provider.http;

public interface ProviderHttpTransport {

    ProviderHttpResponse send(ProviderHttpRequest request);
}
