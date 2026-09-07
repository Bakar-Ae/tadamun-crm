package com.crm.backend.webhook;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookEndpointValidatorTest {

    private static final WebhookSecurityProperties PRODUCTION_PROPERTIES =
            new WebhookSecurityProperties("unused", "v1", false);

    @Test
    void shouldAcceptPublicHttpsEndpoint() throws Exception {
        WebhookEndpointValidator validator = validator(
                PRODUCTION_PROPERTIES,
                Map.of("hooks.example.com", "93.184.216.34")
        );

        assertEquals(
                "https://hooks.example.com/crm/events",
                validator.validateAndNormalize(
                        " https://hooks.example.com/crm/events "
                )
        );
    }

    @Test
    void shouldRejectPlainHttpEndpointInProduction() throws Exception {
        WebhookEndpointValidator validator = validator(
                PRODUCTION_PROPERTIES,
                Map.of("hooks.example.com", "93.184.216.34")
        );

        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "http://hooks.example.com/events"
                )
        );
    }

    @Test
    void shouldRejectPrivateAndMetadataAddresses() throws Exception {
        WebhookEndpointValidator privateValidator = validator(
                PRODUCTION_PROPERTIES,
                Map.of("internal.example.com", "10.10.2.5")
        );
        WebhookEndpointValidator metadataValidator = validator(
                PRODUCTION_PROPERTIES,
                Map.of("metadata.example.com", "169.254.169.254")
        );

        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> privateValidator.validateAndNormalize(
                        "https://internal.example.com/events"
                )
        );
        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> metadataValidator.validateAndNormalize(
                        "https://metadata.example.com/events"
                )
        );
    }

    @Test
    void shouldRejectMixedPublicAndPrivateDnsAnswers() throws Exception {
        WebhookEndpointValidator validator = new WebhookEndpointValidator(
                PRODUCTION_PROPERTIES,
                host -> new InetAddress[]{
                        InetAddress.getByName("93.184.216.34"),
                        InetAddress.getByName("192.168.1.10")
                }
        );

        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "https://hooks.example.com/events"
                )
        );
    }

    @Test
    void shouldRejectCredentialsFragmentsAndUnsupportedHttpsPorts()
            throws Exception {
        WebhookEndpointValidator validator = validator(
                PRODUCTION_PROPERTIES,
                Map.of("hooks.example.com", "93.184.216.34")
        );

        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "https://user:pass@hooks.example.com/events"
                )
        );
        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "https://hooks.example.com/events#secret"
                )
        );
        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "https://hooks.example.com:8443/events"
                )
        );
    }

    @Test
    void shouldAllowOnlyExplicitLocalhostHttpInLocalMode() throws Exception {
        WebhookSecurityProperties localProperties =
                new WebhookSecurityProperties("unused", "v1", true);
        WebhookEndpointValidator validator = new WebhookEndpointValidator(
                localProperties,
                host -> InetAddress.getAllByName(
                        "localhost".equals(host)
                                ? "127.0.0.1"
                                : "192.168.1.20"
                )
        );

        assertEquals(
                "http://localhost:9090/webhooks",
                validator.validateAndNormalize(
                        "http://localhost:9090/webhooks"
                )
        );
        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "http://internal.test:9090/webhooks"
                )
        );
    }

    @Test
    void shouldRejectUnresolvableHost() {
        WebhookEndpointValidator validator = new WebhookEndpointValidator(
                PRODUCTION_PROPERTIES,
                host -> {
                    throw new UnknownHostException(host);
                }
        );

        assertThrows(
                InvalidWebhookEndpointException.class,
                () -> validator.validateAndNormalize(
                        "https://missing.example/events"
                )
        );
    }

    private WebhookEndpointValidator validator(
            WebhookSecurityProperties properties,
            Map<String, String> addresses
    ) {
        return new WebhookEndpointValidator(properties, host -> {
            String address = addresses.get(host);
            if (address == null) {
                throw new UnknownHostException(host);
            }
            return new InetAddress[]{InetAddress.getByName(address)};
        });
    }
}
