package com.crm.backend.webhook;

public interface WebhookHttpTransport {

    WebhookHttpResponse send(WebhookHttpRequest request);
}
