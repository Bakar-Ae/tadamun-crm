package com.crm.backend.integration.provider.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations.whatsapp")
public record WhatsAppCloudProperties(
        String baseUrl,
        int responseTimeoutSeconds
) {
}
