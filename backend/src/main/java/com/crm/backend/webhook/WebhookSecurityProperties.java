package com.crm.backend.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhooks")
public record WebhookSecurityProperties(
        String secretEncryptionKeys,
        String currentKeyVersion,
        boolean allowLocalHttp
) {
}
