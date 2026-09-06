package com.crm.backend.publicapi.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Component
public class PublicApiRateLimiter {

    private static final long WINDOW_SECONDS = 60L;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<Long, Window> windows =
            new ConcurrentHashMap<>();
    private final LongSupplier epochSecondSupplier;

    public PublicApiRateLimiter() {
        this(() -> Instant.now().getEpochSecond());
    }

    PublicApiRateLimiter(LongSupplier epochSecondSupplier) {
        this.epochSecondSupplier = epochSecondSupplier;
    }

    public Decision tryAcquire(Long apiKeyId, int limitPerMinute) {
        if (apiKeyId == null || limitPerMinute < 1) {
            throw new IllegalArgumentException(
                    "API key and positive rate limit are required"
            );
        }

        long now = epochSecondSupplier.getAsLong();
        long currentWindow = now / WINDOW_SECONDS;
        MutableDecision decision = new MutableDecision();

        windows.compute(apiKeyId, (ignored, existing) -> {
            if (existing == null || existing.windowNumber() != currentWindow) {
                decision.allowed = true;
                return new Window(currentWindow, 1);
            }

            if (existing.requestCount() >= limitPerMinute) {
                decision.allowed = false;
                return existing;
            }

            decision.allowed = true;
            return new Window(currentWindow, existing.requestCount() + 1);
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry ->
                    entry.getValue().windowNumber() < currentWindow - 1
            );
        }

        int retryAfter = decision.allowed
                ? 0
                : (int) (WINDOW_SECONDS - (now % WINDOW_SECONDS));
        return new Decision(decision.allowed, retryAfter);
    }

    public record Decision(boolean allowed, int retryAfterSeconds) {
    }

    private record Window(long windowNumber, int requestCount) {
    }

    private static final class MutableDecision {
        private boolean allowed;
    }
}
