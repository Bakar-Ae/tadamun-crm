package com.crm.backend.publicapi.key;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiKeyTokenServiceTest {

    private static final String PEPPER =
            "test-public-api-pepper-that-is-longer-than-thirty-two-bytes";

    private final PublicApiKeyTokenService tokenService =
            new PublicApiKeyTokenService(PEPPER);

    @Test
    void generatedKeyShouldUseExpectedFormatAndVerify() {
        GeneratedPublicApiKey generated = tokenService.generate();

        assertTrue(generated.rawKey().matches(
                "^tdm_live_[A-Za-z0-9_-]{16}\\.[A-Za-z0-9_-]{43}$"
        ));
        assertEquals(64, generated.secretHash().length());
        assertFalse(generated.displayPrefix().contains(
                generated.rawKey().substring(
                        generated.rawKey().indexOf('.') + 1
                )
        ));
        assertEquals(
                generated.publicId(),
                tokenService.extractPublicId(generated.rawKey()).orElseThrow()
        );
        assertTrue(tokenService.matches(
                generated.rawKey(),
                generated.publicId(),
                generated.secretHash()
        ));
    }

    @Test
    void generatedKeysShouldBeUnique() {
        GeneratedPublicApiKey first = tokenService.generate();
        GeneratedPublicApiKey second = tokenService.generate();

        assertNotEquals(first.rawKey(), second.rawKey());
        assertNotEquals(first.secretHash(), second.secretHash());
    }

    @Test
    void verificationShouldRejectWrongSecretAndPepper() {
        GeneratedPublicApiKey generated = tokenService.generate();
        GeneratedPublicApiKey other = tokenService.generate();
        PublicApiKeyTokenService otherPepper =
                new PublicApiKeyTokenService(
                        "a-different-test-pepper-that-is-also-long-enough"
                );

        assertFalse(tokenService.matches(
                other.rawKey(),
                generated.publicId(),
                generated.secretHash()
        ));
        assertFalse(otherPepper.matches(
                generated.rawKey(),
                generated.publicId(),
                generated.secretHash()
        ));
        assertTrue(tokenService.extractPublicId("invalid-key").isEmpty());
    }

    @Test
    void constructorShouldRejectWeakPepper() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PublicApiKeyTokenService("too-short")
        );
    }
}
