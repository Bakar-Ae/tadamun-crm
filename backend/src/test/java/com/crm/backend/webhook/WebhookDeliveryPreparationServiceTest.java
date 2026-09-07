package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookDeliveryPreparationServiceTest {

    @Test
    void shouldRevalidateEndpointAndUseBothUnexpiredSecrets() {
        WebhookDeliveryRepository deliveryRepository = mock(
                WebhookDeliveryRepository.class
        );
        WebhookEndpointValidator endpointValidator = mock(
                WebhookEndpointValidator.class
        );
        WebhookSecretEncryptionService encryptionService = mock(
                WebhookSecretEncryptionService.class
        );
        SubscriptionTimeProvider timeProvider = mock(
                SubscriptionTimeProvider.class
        );
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 14, 0);
        WebhookDelivery delivery = delivery(now.plusHours(1));
        when(deliveryRepository.findForWorkerUpdate(20L))
                .thenReturn(Optional.of(delivery));
        when(endpointValidator.validateAndNormalize(any()))
                .thenReturn("https://hooks.example.com/events");
        when(encryptionService.decrypt(any()))
                .thenReturn("whsec_current", "whsec_previous");
        when(timeProvider.now()).thenReturn(now);
        WebhookDeliveryPreparationService service =
                new WebhookDeliveryPreparationService(
                        deliveryRepository,
                        endpointValidator,
                        encryptionService,
                        timeProvider
                );

        PreparedWebhookDelivery prepared = service.prepare(20L);

        assertEquals(1, prepared.attemptNumber());
        assertEquals(
                java.util.List.of("whsec_current", "whsec_previous"),
                prepared.signingSecrets()
        );
        assertEquals("https://hooks.example.com/events",
                prepared.endpoint().toString());
        assertEquals(WebhookDeliveryStatus.PROCESSING,
                delivery.getStatus());
        assertEquals(now, delivery.getClaimedAt());
        assertTrue(prepared.toString().contains("<redacted>"));
        assertTrue(!prepared.toString().contains("whsec_current"));
        verify(endpointValidator).validateAndNormalize(
                "https://hooks.example.com/events"
        );
        verify(deliveryRepository).saveAndFlush(delivery);
    }

    private WebhookDelivery delivery(LocalDateTime previousSecretExpiry) {
        Organization organization = new Organization();
        organization.setId(42L);
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(10L);
        subscription.setOrganization(organization);
        subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
        subscription.setEndpointUrl("https://hooks.example.com/events");
        subscription.setCurrentSecretCiphertext(new byte[]{1});
        subscription.setCurrentSecretNonce(new byte[12]);
        subscription.setCurrentSecretTag(new byte[16]);
        subscription.setCurrentSecretKeyVersion("v1");
        subscription.setPreviousSecretCiphertext(new byte[]{2});
        subscription.setPreviousSecretNonce(new byte[12]);
        subscription.setPreviousSecretTag(new byte[16]);
        subscription.setPreviousSecretKeyVersion("v1");
        subscription.setPreviousSecretExpiresAt(previousSecretExpiry);
        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId("evt_fixed");
        event.setEventType(WebhookEventType.CUSTOMER_CREATED);
        event.setPayload("{\"id\":\"evt_fixed\"}");
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(20L);
        delivery.setPublicDeliveryId("dlv_fixed");
        delivery.setOrganization(organization);
        delivery.setSubscription(subscription);
        delivery.setEvent(event);
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        return delivery;
    }
}
