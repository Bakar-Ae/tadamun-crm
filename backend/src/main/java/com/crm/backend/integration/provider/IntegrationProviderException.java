package com.crm.backend.integration.provider;

public class IntegrationProviderException extends RuntimeException {

    private final String category;
    private final boolean retryable;

    public IntegrationProviderException(
            String category,
            String message,
            boolean retryable
    ) {
        super(message);
        this.category = category;
        this.retryable = retryable;
    }

    public IntegrationProviderException(
            String category,
            String message,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.category = category;
        this.retryable = retryable;
    }

    public String getCategory() {
        return category;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
