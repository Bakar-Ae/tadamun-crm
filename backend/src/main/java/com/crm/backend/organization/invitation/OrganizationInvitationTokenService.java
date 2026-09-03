package com.crm.backend.organization.invitation;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class OrganizationInvitationTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public GeneratedInvitationToken generate() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return new GeneratedInvitationToken(
                rawToken,
                hash(rawToken)
        );
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Invitation token is required"
            );
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Could not hash invitation token",
                    exception
            );
        }
    }
}
