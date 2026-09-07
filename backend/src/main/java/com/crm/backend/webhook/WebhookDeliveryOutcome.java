package com.crm.backend.webhook;

public enum WebhookDeliveryOutcome {
    SUCCEEDED,
    RETRYABLE_FAILURE,
    TERMINAL_FAILURE
}