package com.crm.backend.integration.security;

import java.util.Objects;

public record EncryptedIntegrationCredentials(
        byte[] ciphertext,
        byte[] nonce,
        byte[] authenticationTag,
        String keyVersion
) {

    public EncryptedIntegrationCredentials {
        Objects.requireNonNull(ciphertext, "Ciphertext is required");
        Objects.requireNonNull(nonce, "Nonce is required");
        Objects.requireNonNull(
                authenticationTag,
                "Authentication tag is required"
        );
        Objects.requireNonNull(keyVersion, "Key version is required");
        if (ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext must not be empty");
        }
        if (nonce.length != 12) {
            throw new IllegalArgumentException("Nonce must contain 12 bytes");
        }
        if (authenticationTag.length != 16) {
            throw new IllegalArgumentException(
                    "Authentication tag must contain 16 bytes"
            );
        }
        if (keyVersion.isBlank()) {
            throw new IllegalArgumentException("Key version is required");
        }
        ciphertext = ciphertext.clone();
        nonce = nonce.clone();
        authenticationTag = authenticationTag.clone();
    }

    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    @Override
    public byte[] nonce() {
        return nonce.clone();
    }

    @Override
    public byte[] authenticationTag() {
        return authenticationTag.clone();
    }
}
