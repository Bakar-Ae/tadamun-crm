package com.crm.backend.webhook;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookRetryPolicyTest {

    private final WebhookRetryPolicy policy = new WebhookRetryPolicy();
    private final LocalDateTime now =
            LocalDateTime.of(2026, 9, 7, 12, 0);

    @Test
    void shouldUseBoundedRetrySchedule() {
        assertEquals(6, policy.maximumAttempts());

        assertEquals(
                now.plusMinutes(1),
                policy.nextAttemptAt(1, now).orElseThrow()
        );
        assertEquals(
                now.plusMinutes(5),
                policy.nextAttemptAt(2, now).orElseThrow()
        );
        assertEquals(
                now.plusMinutes(30),
                policy.nextAttemptAt(3, now).orElseThrow()
        );
        assertEquals(
                now.plusHours(2),
                policy.nextAttemptAt(4, now).orElseThrow()
        );
        assertEquals(
                now.plusHours(12),
                policy.nextAttemptAt(5, now).orElseThrow()
        );
        assertTrue(policy.nextAttemptAt(6, now).isEmpty());
    }

    @Test
    void shouldRejectInvalidAttemptCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.nextAttemptAt(0, now)
        );
    }

    @Test
    void shouldHonorAndBoundRetryAfter() {
        assertEquals(
                now.plusMinutes(10),
                policy.nextAttemptAt(1, now, "600").orElseThrow()
        );
        assertEquals(
                now.plusMinutes(1),
                policy.nextAttemptAt(1, now, "10").orElseThrow()
        );
        assertEquals(
                now.plusHours(12),
                policy.nextAttemptAt(1, now, "999999").orElseThrow()
        );
        assertEquals(
                now.plusMinutes(1),
                policy.nextAttemptAt(1, now, "invalid").orElseThrow()
        );
    }
}
