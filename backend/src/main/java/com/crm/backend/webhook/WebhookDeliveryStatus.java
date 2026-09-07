package com.crm.backend.webhook;

public enum WebhookDeliveryStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    RETRY_SCHEDULED,
    TERMINAL_FAILURE,
    DEAD
}