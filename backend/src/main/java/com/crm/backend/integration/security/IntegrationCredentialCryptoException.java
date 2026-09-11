package com.crm.backend.integration.security;

public class IntegrationCredentialCryptoException extends RuntimeException {

    public IntegrationCredentialCryptoException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
