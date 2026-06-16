package com.crm.backend.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_SECONDS = 15 * 60;

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public void checkBlocked(String email) {
        LoginAttempt attempt = attempts.get(normalize(email));

        if (attempt == null) {
            return;
        }

        if (attempt.blockedUntil != null && Instant.now().isBefore(attempt.blockedUntil)) {
            throw new IllegalArgumentException("Too many failed login attempts. Please try again later.");
        }

        if (attempt.blockedUntil != null && Instant.now().isAfter(attempt.blockedUntil)) {
            attempts.remove(normalize(email));
        }
    }

    public void loginSucceeded(String email) {
        attempts.remove(normalize(email));
    }

    public void loginFailed(String email) {
        String key = normalize(email);

        LoginAttempt attempt = attempts.getOrDefault(key, new LoginAttempt(0, null));
        int failedAttempts = attempt.failedAttempts + 1;

        Instant blockedUntil = failedAttempts >= MAX_FAILED_ATTEMPTS
                ? Instant.now().plusSeconds(BLOCK_DURATION_SECONDS)
                : null;

        attempts.put(key, new LoginAttempt(failedAttempts, blockedUntil));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record LoginAttempt(int failedAttempts, Instant blockedUntil) {
    }
}