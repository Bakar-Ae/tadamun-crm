package com.crm.backend.webhook;

public class InvalidWebhookEndpointException
        extends IllegalArgumentException {

    public InvalidWebhookEndpointException(String message) {
        super(message);
    }
}
