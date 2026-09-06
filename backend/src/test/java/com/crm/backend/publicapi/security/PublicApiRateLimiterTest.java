package com.crm.backend.publicapi.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiRateLimiterTest {

    @Test
    void shouldLimitEachKeyAndResetAtNextWindow() {
        AtomicLong epochSecond = new AtomicLong(120L);
        PublicApiRateLimiter limiter = new PublicApiRateLimiter(
                epochSecond::get
        );

        assertTrue(limiter.tryAcquire(1L, 2).allowed());
        assertTrue(limiter.tryAcquire(1L, 2).allowed());

        PublicApiRateLimiter.Decision rejected =
                limiter.tryAcquire(1L, 2);
        assertFalse(rejected.allowed());
        assertTrue(rejected.retryAfterSeconds() > 0);
        assertTrue(limiter.tryAcquire(2L, 2).allowed());

        epochSecond.set(180L);
        assertTrue(limiter.tryAcquire(1L, 2).allowed());
    }
}
