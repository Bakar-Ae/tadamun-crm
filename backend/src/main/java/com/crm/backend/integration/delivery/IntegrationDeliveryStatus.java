package com.crm.backend.integration.delivery;

public enum IntegrationDeliveryStatus {
    PENDING,
    PROCESSING,
    RETRY_SCHEDULED,
    SUCCEEDED,
    TERMINAL_FAILURE,
    DEAD,
    CANCELLED
}
