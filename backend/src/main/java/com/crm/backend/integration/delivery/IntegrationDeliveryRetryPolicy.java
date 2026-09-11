package com.crm.backend.integration.delivery;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class IntegrationDeliveryRetryPolicy {

    private static final List<Duration> DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(12),
            Duration.ofHours(24),
            Duration.ofHours(48),
            Duration.ofHours(72),
            Duration.ofHours(96)
    );

    public LocalDateTime nextAttemptAt(
            int completedAttempts,
            LocalDateTime now
    ) {
        int index = Math.max(0, Math.min(
                completedAttempts - 1,
                DELAYS.size() - 1
        ));
        return now.plus(DELAYS.get(index));
    }
}
