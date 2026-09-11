package com.crm.backend.integration.provider;

import com.crm.backend.integration.IntegrationProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationProviderRegistryTest {

    @Test
    void shouldRegisterAndResolveProviderAdapter() {
        IntegrationProviderAdapter adapter = adapter(
                IntegrationProvider.WHATSAPP_CLOUD
        );
        IntegrationProviderRegistry registry =
                new IntegrationProviderRegistry(List.of(adapter));

        assertTrue(registry.supports(IntegrationProvider.WHATSAPP_CLOUD));
        assertFalse(registry.supports(IntegrationProvider.SMTP));
        assertSame(
                adapter,
                registry.require(IntegrationProvider.WHATSAPP_CLOUD)
        );
        assertThrows(
                IllegalStateException.class,
                () -> registry.require(IntegrationProvider.SMTP)
        );
    }

    @Test
    void shouldRejectDuplicateProviderAdapters() {
        assertThrows(
                IllegalStateException.class,
                () -> new IntegrationProviderRegistry(List.of(
                        adapter(IntegrationProvider.SMTP),
                        adapter(IntegrationProvider.SMTP)
                ))
        );
    }

    private IntegrationProviderAdapter adapter(
            IntegrationProvider provider
    ) {
        return new IntegrationProviderAdapter() {
            @Override
            public IntegrationProvider provider() {
                return provider;
            }

            @Override
            public Set<IntegrationCapability> capabilities() {
                return Set.of(IntegrationCapability.TEST_CONNECTION);
            }

            @Override
            public void validateConfiguration(
                    Map<String, Object> configuration
            ) {
            }

            @Override
            public void validateCredentials(IntegrationSecrets secrets) {
            }

            @Override
            public IntegrationVerificationResult verify(
                    IntegrationProviderContext context
            ) {
                return IntegrationVerificationResult.success("test-account");
            }

            @Override
            public IntegrationProviderDeliveryResult deliver(
                    IntegrationProviderContext context,
                    IntegrationOutboundMessage message
            ) {
                return new IntegrationProviderDeliveryResult("test-message");
            }
        };
    }
}
