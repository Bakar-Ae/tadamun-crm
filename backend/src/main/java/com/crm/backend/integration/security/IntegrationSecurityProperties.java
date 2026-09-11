package com.crm.backend.integration.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations")
public record IntegrationSecurityProperties(
        String credentialEncryptionKeys,
        String currentKeyVersion
) {
}
