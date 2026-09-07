package com.crm.backend.webhook;

import java.util.Objects;

public final class GeneratedWebhookSecret {

    private final String rawSecret;
    private final String displaySuffix;
    private final EncryptedWebhookSecret encryptedSecret;

    public GeneratedWebhookSecret(
            String rawSecret,
            String displaySuffix,
            EncryptedWebhookSecret encryptedSecret
    ) {
        this.rawSecret = Objects.requireNonNull(
                rawSecret,
                "Raw secret is required"
        );
        this.displaySuffix = Objects.requireNonNull(
                displaySuffix,
                "Display suffix is required"
        );
        this.encryptedSecret = Objects.requireNonNull(
                encryptedSecret,
                "Encrypted secret is required"
        );
    }

    public String rawSecret() {
        return rawSecret;
    }

    public String displaySuffix() {
        return displaySuffix;
    }

    public EncryptedWebhookSecret encryptedSecret() {
        return encryptedSecret;
    }

    @Override
    public String toString() {
        return "GeneratedWebhookSecret[rawSecret=<redacted>, "
                + "displaySuffix=" + displaySuffix
                + ", keyVersion=" + encryptedSecret.keyVersion() + "]";
    }
}
