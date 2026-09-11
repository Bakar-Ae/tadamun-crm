package com.crm.backend.integration.provider.smtp;

import com.crm.backend.integration.provider.IntegrationSecrets;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmtpIntegrationAdapterTest {

    private final SmtpIntegrationAdapter adapter =
            new SmtpIntegrationAdapter();

    @Test
    void shouldValidateSupportedSmtpConfiguration() {
        assertDoesNotThrow(() -> adapter.validateConfiguration(Map.of(
                "host", "mail.example.com",
                "port", 587,
                "fromAddress", "sales@example.com",
                "smtpAuth", true,
                "startTls", true
        )));
        assertDoesNotThrow(() -> adapter.validateCredentials(
                new IntegrationSecrets(Map.of(
                        "username", "sales@example.com",
                        "password", "test-password"
                ))
        ));
    }

    @Test
    void shouldRejectInvalidHostAndSender() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.validateConfiguration(Map.of(
                        "host", "https://mail.example.com",
                        "port", 587,
                        "fromAddress", "not-an-email"
                ))
        );
    }
}
