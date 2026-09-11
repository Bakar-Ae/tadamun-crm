package com.crm.backend.integration.provider.whatsapp;

import com.crm.backend.integration.provider.IntegrationDeliveryType;
import com.crm.backend.integration.provider.IntegrationOutboundMessage;
import com.crm.backend.integration.provider.IntegrationProviderContext;
import com.crm.backend.integration.provider.IntegrationProviderException;
import com.crm.backend.integration.provider.IntegrationSecrets;
import com.crm.backend.integration.provider.http.ProviderHttpRequest;
import com.crm.backend.integration.provider.http.ProviderHttpResponse;
import com.crm.backend.integration.provider.http.ProviderHttpTransport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppCloudAdapterTest {

    @Test
    void shouldVerifyAndDeliverWithoutExposingAccessToken() {
        CapturingTransport transport = new CapturingTransport();
        WhatsAppCloudAdapter adapter = adapter(transport);
        IntegrationProviderContext context = context();

        transport.response = new ProviderHttpResponse(
                200,
                "{\"id\":\"123456789\"}"
        );
        assertEquals(
                "123456789",
                adapter.verify(context).externalAccountId()
        );
        assertEquals("GET", transport.request.method());

        transport.response = new ProviderHttpResponse(
                200,
                "{\"messages\":[{\"id\":\"wamid.test\"}]}"
        );
        var result = adapter.deliver(
                context,
                new IntegrationOutboundMessage(
                        IntegrationDeliveryType.WHATSAPP_TEXT,
                        "+252612345678",
                        null,
                        "Hello from Tadamun",
                        "phase88-test"
                )
        );

        assertEquals("wamid.test", result.providerMessageId());
        assertEquals("POST", transport.request.method());
        assertTrue(transport.request.uri().toString().endsWith(
                "/v23.0/123456789/messages"
        ));
        assertTrue(transport.request.body().contains("252612345678"));
        assertFalse(transport.request.toString().contains("test-token"));
    }

    @Test
    void shouldClassifyServerFailureAsRetryable() {
        CapturingTransport transport = new CapturingTransport();
        transport.response = new ProviderHttpResponse(503, "unavailable");

        IntegrationProviderException failure = assertThrows(
                IntegrationProviderException.class,
                () -> adapter(transport).verify(context())
        );

        assertEquals("HTTP_503", failure.getCategory());
        assertTrue(failure.isRetryable());
    }

    private WhatsAppCloudAdapter adapter(ProviderHttpTransport transport) {
        return new WhatsAppCloudAdapter(
                transport,
                new ObjectMapper(),
                new WhatsAppCloudProperties(
                        "https://graph.facebook.com",
                        10
                )
        );
    }

    private IntegrationProviderContext context() {
        return new IntegrationProviderContext(
                1L,
                "int_phase88_test",
                Map.of(
                        "phoneNumberId", "123456789",
                        "apiVersion", "v23.0"
                ),
                new IntegrationSecrets(Map.of(
                        "accessToken", "test-token"
                ))
        );
    }

    private static final class CapturingTransport
            implements ProviderHttpTransport {

        private ProviderHttpRequest request;
        private ProviderHttpResponse response;

        @Override
        public ProviderHttpResponse send(ProviderHttpRequest request) {
            this.request = request;
            return response;
        }
    }
}
