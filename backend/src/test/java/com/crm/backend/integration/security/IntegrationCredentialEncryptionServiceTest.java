package com.crm.backend.integration.security;

import com.crm.backend.integration.IntegrationProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegrationCredentialEncryptionServiceTest {

    private static final String KEY_V1 =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String KEY_V2 =
            "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final IntegrationCredentialContext CONTEXT =
            new IntegrationCredentialContext(
                    1L,
                    "int_phase88_primary",
                    IntegrationProvider.WHATSAPP_CLOUD
            );

    @Test
    void shouldEncryptAndDecryptCredentialsWithUniqueNonces() {
        IntegrationCredentialEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );

        EncryptedIntegrationCredentials first = service.encrypt(
                CONTEXT,
                "{\"accessToken\":\"test-secret\"}"
        );
        EncryptedIntegrationCredentials second = service.encrypt(
                CONTEXT,
                "{\"accessToken\":\"test-secret\"}"
        );

        assertFalse(Arrays.equals(first.nonce(), second.nonce()));
        assertFalse(Arrays.equals(first.ciphertext(), second.ciphertext()));
        assertEquals(
                "{\"accessToken\":\"test-secret\"}",
                service.decrypt(CONTEXT, first)
        );
    }

    @Test
    void shouldRejectCiphertextMovedToAnotherTenantOrConnection() {
        IntegrationCredentialEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );
        EncryptedIntegrationCredentials encrypted = service.encrypt(
                CONTEXT,
                "{\"accessToken\":\"test-secret\"}"
        );

        IntegrationCredentialContext anotherTenant =
                new IntegrationCredentialContext(
                        2L,
                        CONTEXT.publicConnectionId(),
                        CONTEXT.provider()
                );
        IntegrationCredentialContext anotherConnection =
                new IntegrationCredentialContext(
                        CONTEXT.organizationId(),
                        "int_phase88_other",
                        CONTEXT.provider()
                );

        assertThrows(
                IntegrationCredentialCryptoException.class,
                () -> service.decrypt(anotherTenant, encrypted)
        );
        assertThrows(
                IntegrationCredentialCryptoException.class,
                () -> service.decrypt(anotherConnection, encrypted)
        );
    }

    @Test
    void shouldRejectTampering() {
        IntegrationCredentialEncryptionService service = service(
                "v1:" + KEY_V1,
                "v1"
        );
        EncryptedIntegrationCredentials encrypted = service.encrypt(
                CONTEXT,
                "{\"password\":\"test-secret\"}"
        );
        byte[] tag = encrypted.authenticationTag();
        tag[0] ^= 1;

        EncryptedIntegrationCredentials tampered =
                new EncryptedIntegrationCredentials(
                        encrypted.ciphertext(),
                        encrypted.nonce(),
                        tag,
                        encrypted.keyVersion()
                );

        assertThrows(
                IntegrationCredentialCryptoException.class,
                () -> service.decrypt(CONTEXT, tampered)
        );
    }

    @Test
    void shouldDecryptOldCredentialsAfterKeyRotation() {
        IntegrationCredentialEncryptionService oldService = service(
                "v1:" + KEY_V1,
                "v1"
        );
        EncryptedIntegrationCredentials oldValue = oldService.encrypt(
                CONTEXT,
                "{\"token\":\"old-secret\"}"
        );
        IntegrationCredentialEncryptionService rotatedService = service(
                "v1:" + KEY_V1 + ",v2:" + KEY_V2,
                "v2"
        );

        assertEquals(
                "{\"token\":\"old-secret\"}",
                rotatedService.decrypt(CONTEXT, oldValue)
        );
        assertEquals(
                "v2",
                rotatedService.encrypt(CONTEXT, "new-secret").keyVersion()
        );
    }

    @Test
    void shouldRejectInvalidKeyConfiguration() {
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
                () -> service(
                        "v1:" + KEY_V1 + ",v1:" + KEY_V2,
                        "v1"
                )
        );
    }

    private IntegrationCredentialEncryptionService service(
            String keys,
            String currentVersion
    ) {
        return new IntegrationCredentialEncryptionService(
                new IntegrationSecurityProperties(keys, currentVersion)
        );
    }
}
