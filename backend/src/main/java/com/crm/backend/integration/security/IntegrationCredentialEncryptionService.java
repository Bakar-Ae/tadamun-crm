package com.crm.backend.integration.security;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class IntegrationCredentialEncryptionService {

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String AAD_PREFIX = "tadamun:integration:";
    private static final int AES_KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BYTES = 16;
    private static final int TAG_BITS = 128;
    private static final Pattern KEY_VERSION_PATTERN = Pattern.compile(
            "[A-Za-z0-9._-]{1,50}"
    );

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKey> encryptionKeys;
    private final String currentKeyVersion;

    public IntegrationCredentialEncryptionService(
            IntegrationSecurityProperties properties
    ) {
        this.encryptionKeys = parseKeyRing(
                properties.credentialEncryptionKeys()
        );
        this.currentKeyVersion = validateCurrentVersion(
                properties.currentKeyVersion(),
                encryptionKeys
        );
    }

    public EncryptedIntegrationCredentials encrypt(
            IntegrationCredentialContext context,
            String rawCredentials
    ) {
        if (rawCredentials == null || rawCredentials.isBlank()) {
            throw new IllegalArgumentException(
                    "Integration credentials are required"
            );
        }

        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] plaintext = rawCredentials.getBytes(StandardCharsets.UTF_8);

        try {
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    encryptionKeys.get(currentKeyVersion),
                    new GCMParameterSpec(TAG_BITS, nonce)
            );
            cipher.updateAAD(aad(currentKeyVersion, context));
            byte[] encryptedWithTag = cipher.doFinal(plaintext);
            int tagOffset = encryptedWithTag.length - TAG_BYTES;
            return new EncryptedIntegrationCredentials(
                    Arrays.copyOf(encryptedWithTag, tagOffset),
                    nonce,
                    Arrays.copyOfRange(
                            encryptedWithTag,
                            tagOffset,
                            encryptedWithTag.length
                    ),
                    currentKeyVersion
            );
        } catch (GeneralSecurityException exception) {
            throw new IntegrationCredentialCryptoException(
                    "Could not encrypt integration credentials",
                    exception
            );
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public String decrypt(
            IntegrationCredentialContext context,
            EncryptedIntegrationCredentials encryptedCredentials
    ) {
        if (encryptedCredentials == null) {
            throw new IllegalArgumentException(
                    "Encrypted integration credentials are required"
            );
        }

        SecretKey key = encryptionKeys.get(encryptedCredentials.keyVersion());
        if (key == null) {
            throw new IllegalStateException(
                    "No integration encryption key configured for version: "
                            + encryptedCredentials.keyVersion()
            );
        }

        byte[] ciphertext = encryptedCredentials.ciphertext();
        byte[] tag = encryptedCredentials.authenticationTag();
        byte[] encryptedWithTag = new byte[ciphertext.length + tag.length];
        System.arraycopy(ciphertext, 0, encryptedWithTag, 0,
                ciphertext.length);
        System.arraycopy(tag, 0, encryptedWithTag, ciphertext.length,
                tag.length);

        byte[] plaintext = null;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            TAG_BITS,
                            encryptedCredentials.nonce()
                    )
            );
            cipher.updateAAD(aad(
                    encryptedCredentials.keyVersion(),
                    context
            ));
            plaintext = cipher.doFinal(encryptedWithTag);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IntegrationCredentialCryptoException(
                    "Could not decrypt integration credentials",
                    exception
            );
        } finally {
            Arrays.fill(ciphertext, (byte) 0);
            Arrays.fill(tag, (byte) 0);
            Arrays.fill(encryptedWithTag, (byte) 0);
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private static Map<String, SecretKey> parseKeyRing(String configuredKeys) {
        if (configuredKeys == null || configuredKeys.isBlank()) {
            throw new IllegalStateException(
                    "Integration credential encryption keys are required"
            );
        }

        Map<String, SecretKey> parsedKeys = new LinkedHashMap<>();
        for (String configuredKey : configuredKeys.split(",")) {
            String entry = configuredKey.trim();
            int delimiter = entry.indexOf(':');
            if (delimiter <= 0 || delimiter == entry.length() - 1) {
                throw new IllegalStateException(
                        "Invalid integration encryption key-ring entry"
                );
            }

            String version = entry.substring(0, delimiter).trim();
            String encodedKey = entry.substring(delimiter + 1).trim();
            validateKeyVersion(version);
            byte[] decodedKey;
            try {
                decodedKey = Base64.getDecoder().decode(encodedKey);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException(
                        "Integration encryption key is not valid Base64 for "
                                + "version: " + version,
                        exception
                );
            }

            try {
                if (decodedKey.length != AES_KEY_BYTES) {
                    throw new IllegalStateException(
                            "Integration encryption key must contain 32 bytes "
                                    + "for version: " + version
                    );
                }
                if (parsedKeys.putIfAbsent(
                        version,
                        new SecretKeySpec(decodedKey, "AES")
                ) != null) {
                    throw new IllegalStateException(
                            "Duplicate integration encryption key version: "
                                    + version
                    );
                }
            } finally {
                Arrays.fill(decodedKey, (byte) 0);
            }
        }
        return Collections.unmodifiableMap(parsedKeys);
    }

    private static String validateCurrentVersion(
            String configuredVersion,
            Map<String, SecretKey> keys
    ) {
        validateKeyVersion(configuredVersion);
        if (!keys.containsKey(configuredVersion)) {
            throw new IllegalStateException(
                    "Current integration encryption key version is not "
                            + "configured: " + configuredVersion
            );
        }
        return configuredVersion;
    }

    private static void validateKeyVersion(String version) {
        if (version == null
                || !KEY_VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalStateException(
                    "Invalid integration encryption key version"
            );
        }
    }

    private static byte[] aad(
            String keyVersion,
            IntegrationCredentialContext context
    ) {
        String value = AAD_PREFIX
                + keyVersion + ':'
                + context.organizationId() + ':'
                + context.publicConnectionId() + ':'
                + context.provider().name();
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
