package com.crm.backend.integration.delivery;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationDeliveryRetryPolicyTest {

    private final IntegrationDeliveryRetryPolicy policy =
            new IntegrationDeliveryRetryPolicy();
    private final LocalDateTime now = LocalDateTime.of(
            2026, 9, 10, 12, 0
    );

    @Test
    void shouldUseBoundedExponentialBackoff() {
        assertEquals(now.plusMinutes(1), policy.nextAttemptAt(1, now));
        assertEquals(now.plusMinutes(5), policy.nextAttemptAt(2, now));
        assertEquals(now.plusHours(2), policy.nextAttemptAt(4, now));
        assertEquals(now.plusHours(96), policy.nextAttemptAt(20, now));
    }
}
