package com.crm.backend.webhook;

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
public class WebhookSecretEncryptionService {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SECRET_PREFIX = "whsec_";
    private static final String AAD_PREFIX = "tadamun:webhook-secret:";
    private static final int AES_KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int AUTHENTICATION_TAG_BYTES = 16;
    private static final int AUTHENTICATION_TAG_BITS = 128;
    private static final int GENERATED_SECRET_BYTES = 32;
    private static final int DISPLAY_SUFFIX_LENGTH = 8;
    private static final Pattern KEY_VERSION_PATTERN = Pattern.compile(
            "[A-Za-z0-9._-]{1,50}"
    );

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKey> encryptionKeys;
    private final String currentKeyVersion;

    public WebhookSecretEncryptionService(
            WebhookSecurityProperties properties
    ) {
        this.encryptionKeys = parseKeyRing(
                properties.secretEncryptionKeys()
        );
        this.currentKeyVersion = validateCurrentVersion(
                properties.currentKeyVersion(),
                encryptionKeys
        );
    }

    public GeneratedWebhookSecret generate() {
        byte[] randomBytes = new byte[GENERATED_SECRET_BYTES];
        secureRandom.nextBytes(randomBytes);

        try {
            String rawSecret = SECRET_PREFIX
                    + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(randomBytes);
            String displaySuffix = rawSecret.substring(
                    rawSecret.length() - DISPLAY_SUFFIX_LENGTH
            );
            return new GeneratedWebhookSecret(
                    rawSecret,
                    displaySuffix,
                    encrypt(rawSecret)
            );
        } finally {
            Arrays.fill(randomBytes, (byte) 0);
        }
    }

    public EncryptedWebhookSecret encrypt(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret is required");
        }

        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] plaintext = rawSecret.getBytes(StandardCharsets.UTF_8);

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    encryptionKeys.get(currentKeyVersion),
                    new GCMParameterSpec(AUTHENTICATION_TAG_BITS, nonce)
            );
            cipher.updateAAD(aad(currentKeyVersion));
            byte[] encryptedWithTag = cipher.doFinal(plaintext);
            int tagOffset = encryptedWithTag.length
                    - AUTHENTICATION_TAG_BYTES;

            return new EncryptedWebhookSecret(
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
            throw new WebhookSecretCryptoException(
                    "Could not encrypt webhook secret",
                    exception
            );
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public String decrypt(EncryptedWebhookSecret encryptedSecret) {
        if (encryptedSecret == null) {
            throw new IllegalArgumentException(
                    "Encrypted webhook secret is required"
            );
        }

        SecretKey key = encryptionKeys.get(encryptedSecret.keyVersion());
        if (key == null) {
            throw new IllegalStateException(
                    "No webhook encryption key configured for version: "
                            + encryptedSecret.keyVersion()
            );
        }

        byte[] ciphertext = encryptedSecret.ciphertext();
        byte[] authenticationTag = encryptedSecret.authenticationTag();
        byte[] encryptedWithTag = new byte[
                ciphertext.length + authenticationTag.length
        ];
        System.arraycopy(
                ciphertext,
                0,
                encryptedWithTag,
                0,
                ciphertext.length
        );
        System.arraycopy(
                authenticationTag,
                0,
                encryptedWithTag,
                ciphertext.length,
                authenticationTag.length
        );

        byte[] plaintext = null;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_BITS,
                            encryptedSecret.nonce()
                    )
            );
            cipher.updateAAD(aad(encryptedSecret.keyVersion()));
            plaintext = cipher.doFinal(encryptedWithTag);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new WebhookSecretCryptoException(
                    "Could not decrypt webhook secret",
                    exception
            );
        } finally {
            Arrays.fill(ciphertext, (byte) 0);
            Arrays.fill(authenticationTag, (byte) 0);
            Arrays.fill(encryptedWithTag, (byte) 0);
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private static Map<String, SecretKey> parseKeyRing(String configuredKeys) {
        if (configuredKeys == null || configuredKeys.isBlank()) {
            throw new IllegalStateException(
                    "Webhook secret encryption keys are required"
            );
        }

        Map<String, SecretKey> parsedKeys = new LinkedHashMap<>();
        for (String configuredKey : configuredKeys.split(",")) {
            String entry = configuredKey.trim();
            int delimiter = entry.indexOf(':');
            if (delimiter <= 0 || delimiter == entry.length() - 1) {
                throw new IllegalStateException(
                        "Invalid webhook encryption key-ring entry"
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
                        "Webhook encryption key is not valid Base64 for version: "
                                + version,
                        exception
                );
            }

            try {
                if (decodedKey.length != AES_KEY_BYTES) {
                    throw new IllegalStateException(
                            "Webhook encryption key must contain 32 bytes for version: "
                                    + version
                    );
                }
                if (parsedKeys.putIfAbsent(
                        version,
                        new SecretKeySpec(decodedKey, "AES")
                ) != null) {
                    throw new IllegalStateException(
                            "Duplicate webhook encryption key version: "
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
                    "Current webhook encryption key version is not configured: "
                            + configuredVersion
            );
        }
        return configuredVersion;
    }

    private static void validateKeyVersion(String version) {
        if (version == null
                || !KEY_VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalStateException(
                    "Invalid webhook encryption key version"
            );
        }
    }

    private static byte[] aad(String keyVersion) {
        return (AAD_PREFIX + keyVersion).getBytes(StandardCharsets.UTF_8);
    }
}
