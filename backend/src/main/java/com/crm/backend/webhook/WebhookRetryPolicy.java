package com.crm.backend.webhook;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class WebhookRetryPolicy {

    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(12)
    );
    private static final Duration MAXIMUM_RETRY_AFTER =
            Duration.ofHours(12);

    public int maximumAttempts() {
        return RETRY_DELAYS.size() + 1;
    }

    public Optional<LocalDateTime> nextAttemptAt(
            int completedAttempts,
            LocalDateTime now
    ) {
        return nextAttemptAt(completedAttempts, now, null);
    }

    public Optional<LocalDateTime> nextAttemptAt(
            int completedAttempts,
            LocalDateTime now,
            String retryAfter
    ) {
        if (completedAttempts < 1) {
            throw new IllegalArgumentException(
                    "Completed attempts must be positive"
            );
        }

        if (completedAttempts >= maximumAttempts()) {
            return Optional.empty();
        }

        Duration normalDelay = RETRY_DELAYS.get(completedAttempts - 1);
        Duration requestedDelay = parseRetryAfter(retryAfter, now)
                .orElse(normalDelay);
        Duration boundedDelay = requestedDelay.compareTo(normalDelay) < 0
                ? normalDelay
                : requestedDelay;
        if (boundedDelay.compareTo(MAXIMUM_RETRY_AFTER) > 0) {
            boundedDelay = MAXIMUM_RETRY_AFTER;
        }
        return Optional.of(now.plus(boundedDelay));
    }

    private Optional<Duration> parseRetryAfter(
            String value,
            LocalDateTime now
    ) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim();
        try {
            long seconds = Long.parseLong(normalized);
            return seconds < 0
                    ? Optional.empty()
                    : Optional.of(Duration.ofSeconds(seconds));
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime retryAt = ZonedDateTime.parse(
                        normalized,
                        DateTimeFormatter.RFC_1123_DATE_TIME
                );
                Duration delay = Duration.between(
                        now.toInstant(ZoneOffset.UTC),
                        retryAt.toInstant()
                );
                return delay.isNegative()
                        ? Optional.empty()
                        : Optional.of(delay);
            } catch (DateTimeParseException invalidDate) {
                return Optional.empty();
            }
        }
    }
}
