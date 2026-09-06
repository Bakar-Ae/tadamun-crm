package com.crm.backend.subscription.billing;

public class BillingUnavailableException extends RuntimeException {

    public BillingUnavailableException(String message) {
        super(message);
    }
}
