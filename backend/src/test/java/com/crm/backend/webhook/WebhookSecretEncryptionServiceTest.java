package com.crm.backend.webhook;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSecretEncryptionServiceTest {

    private static final String KEY_V1 =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String KEY_V2 =
            "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Test
    void generatedSecretShouldUseExpectedFormatAndDecrypt() {
        WebhookSecretEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );

        GeneratedWebhookSecret generated = service.generate();

        assertTrue(generated.rawSecret().matches(
                "^whsec_[A-Za-z0-9_-]{43}$"
        ));
        assertEquals(
                generated.rawSecret().substring(
                        generated.rawSecret().length() - 8
                ),
                generated.displaySuffix()
        );
        assertEquals(
                generated.rawSecret(),
                service.decrypt(generated.encryptedSecret())
        );
        assertEquals("v1", generated.encryptedSecret().keyVersion());
    }

    @Test
    void repeatedEncryptionShouldUseDifferentNoncesAndCiphertext() {
        WebhookSecretEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );

        EncryptedWebhookSecret first = service.encrypt("whsec_example");
        EncryptedWebhookSecret second = service.encrypt("whsec_example");

        assertFalse(Arrays.equals(first.nonce(), second.nonce()));
        assertFalse(Arrays.equals(first.ciphertext(), second.ciphertext()));
        assertEquals("whsec_example", service.decrypt(first));
        assertEquals("whsec_example", service.decrypt(second));
    }

    @Test
    void tamperedAuthenticationTagShouldBeRejected() {
        WebhookSecretEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );
        EncryptedWebhookSecret encrypted = service.encrypt("whsec_example");
        byte[] tamperedTag = encrypted.authenticationTag();
        tamperedTag[0] ^= 1;
        EncryptedWebhookSecret tampered = new EncryptedWebhookSecret(
                encrypted.ciphertext(),
                encrypted.nonce(),
                tamperedTag,
                encrypted.keyVersion()
        );

        assertThrows(
                WebhookSecretCryptoException.class,
                () -> service.decrypt(tampered)
        );
    }

    @Test
    void keyRingShouldDecryptOldValuesAfterKeyRotation() {
        WebhookSecretEncryptionService oldService = service(
                "v1:" + KEY_V1,
                "v1"
        );
        EncryptedWebhookSecret oldValue = oldService.encrypt(
                "whsec_old_value"
        );
        WebhookSecretEncryptionService rotatedService = service(
                "v1:" + KEY_V1 + ",v2:" + KEY_V2,
                "v2"
        );

        assertEquals("whsec_old_value", rotatedService.decrypt(oldValue));
        assertEquals("v2", rotatedService.encrypt("new-value").keyVersion());
    }

    @Test
    void encryptedValueShouldDefensivelyCopyByteArrays() {
        WebhookSecretEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );
        EncryptedWebhookSecret encrypted = service.encrypt("whsec_example");
        byte[] externalNonce = encrypted.nonce();
        externalNonce[0] ^= 1;

        assertEquals("whsec_example", service.decrypt(encrypted));
    }

    @Test
    void generatedSecretToStringShouldRedactRawSecret() {
        GeneratedWebhookSecret generated = service(
                "v1:" + KEY_V1,
                "v1"
        ).generate();

        assertFalse(generated.toString().contains(generated.rawSecret()));
        assertTrue(generated.toString().contains("<redacted>"));
    }

    @Test
    void invalidKeyRingAndUnknownCurrentVersionShouldBeRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> service("v1:c2hvcnQ=", "v1")
        );
        assertThrows(
                IllegalStateException.class,
                () -> service("v1:" + KEY_V1, "v2")
        );
        assertThrows(
                IllegalStateException.class,
                () -> service("v1:" + KEY_V1 + ",v1:" + KEY_V2, "v1")
        );
    }

    private WebhookSecretEncryptionService service(
            String keys,
            String currentVersion
    ) {
        return new WebhookSecretEncryptionService(
                new WebhookSecurityProperties(
                        keys,
                        currentVersion,
                        true
                )
        );
    }
}
