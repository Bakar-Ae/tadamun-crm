package com.crm.backend.integration.provider;

public record IntegrationVerificationResult(
        boolean successful,
        String externalAccountId,
        String message
) {

    public static IntegrationVerificationResult success(
            String externalAccountId
    ) {
        return new IntegrationVerificationResult(
                true,
                externalAccountId,
                "Connection verified"
        );
    }

    public static IntegrationVerificationResult failure(String message) {
        return new IntegrationVerificationResult(false, null, message);
    }
}
