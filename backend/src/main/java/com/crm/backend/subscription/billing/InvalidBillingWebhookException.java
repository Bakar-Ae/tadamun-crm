package com.crm.backend.subscription.billing;

public class InvalidBillingWebhookException extends RuntimeException {

    public InvalidBillingWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
