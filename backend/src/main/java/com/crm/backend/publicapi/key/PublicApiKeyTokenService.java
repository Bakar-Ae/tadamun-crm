package com.crm.backend.publicapi.key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicApiKeyTokenService {

    private static final String KEY_PREFIX = "tdm_live_";
    private static final int PUBLIC_ID_BYTES = 12;
    private static final int SECRET_BYTES = 32;
    private static final int DISPLAY_SECRET_LENGTH = 6;
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^tdm_live_([A-Za-z0-9_-]{16})\\.([A-Za-z0-9_-]{43})$"
    );
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec pepperKey;

    public PublicApiKeyTokenService(
            @Value("${app.public-api.key-pepper}") String pepper
    ) {
        if (pepper == null
                || pepper.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "Public API key pepper must contain at least 32 bytes"
            );
        }

        pepperKey = new SecretKeySpec(
                pepper.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    public GeneratedPublicApiKey generate() {
        String publicId = randomUrlToken(PUBLIC_ID_BYTES);
        String secret = randomUrlToken(SECRET_BYTES);
        String rawKey = KEY_PREFIX + publicId + "." + secret;
        String displayPrefix = KEY_PREFIX
                + publicId
                + "."
                + secret.substring(0, DISPLAY_SECRET_LENGTH)
                + "...";

        return new GeneratedPublicApiKey(
                rawKey,
                publicId,
                displayPrefix,
                hash(publicId, secret)
        );
    }

    public Optional<String> extractPublicId(String rawKey) {
        Matcher matcher = matcher(rawKey);
        return matcher == null
                ? Optional.empty()
                : Optional.of(matcher.group(1));
    }

    public boolean matches(
            String rawKey,
            String expectedPublicId,
            String expectedHash
    ) {
        Matcher matcher = matcher(rawKey);

        if (matcher == null
                || expectedPublicId == null
                || expectedHash == null
                || !matcher.group(1).equals(expectedPublicId)) {
            return false;
        }

        String actualHash = hash(matcher.group(1), matcher.group(2));
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                actualHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private Matcher matcher(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }

        Matcher matcher = KEY_PATTERN.matcher(rawKey.trim());
        return matcher.matches() ? matcher : null;
    }

    private String randomUrlToken(int byteCount) {
        byte[] bytes = new byte[byteCount];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String publicId, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(pepperKey);
            byte[] digest = mac.doFinal(
                    (publicId + "." + secret)
                            .getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not hash public API key",
                    exception
            );
        }
    }
}
