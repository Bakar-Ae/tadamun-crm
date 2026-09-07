package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookEventFanoutServiceTest {

    @Test
    void shouldCreateOnlyMissingDeliveriesAndPublishEvent() {
        WebhookEventRepository eventRepository = mock(
                WebhookEventRepository.class
        );
        WebhookSubscriptionRepository subscriptionRepository = mock(
                WebhookSubscriptionRepository.class
        );
        WebhookDeliveryRepository deliveryRepository = mock(
                WebhookDeliveryRepository.class
        );
        WebhookPublicIdGenerator idGenerator = mock(
                WebhookPublicIdGenerator.class
        );
        SubscriptionTimeProvider timeProvider = mock(
                SubscriptionTimeProvider.class
        );
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 13, 0);
        Organization organization = new Organization();
        organization.setId(42L);
        WebhookEvent event = new WebhookEvent();
        event.setId(5L);
        event.setOrganization(organization);
        event.setEventType(WebhookEventType.LEAD_UPDATED);
        event.setPublicationStatus(WebhookPublicationStatus.PENDING);
        WebhookSubscription existing = subscription(10L, organization);
        WebhookSubscription missing = subscription(11L, organization);

        when(eventRepository.findForPublicationUpdate(5L))
                .thenReturn(Optional.of(event));
        when(subscriptionRepository.findMatchingSubscriptions(
                42L,
                WebhookSubscriptionStatus.ACTIVE,
                WebhookEventType.LEAD_UPDATED
        )).thenReturn(List.of(existing, missing));
        when(deliveryRepository.existsByEventIdAndSubscriptionId(5L, 10L))
                .thenReturn(true);
        when(deliveryRepository.existsByEventIdAndSubscriptionId(5L, 11L))
                .thenReturn(false);
        when(idGenerator.deliveryId()).thenReturn("dlv_fixed");
        when(timeProvider.now()).thenReturn(now);
        when(deliveryRepository.save(any(WebhookDelivery.class)))
                .thenAnswer(invocation -> {
                    WebhookDelivery delivery = invocation.getArgument(0);
                    delivery.setId(101L);
                    return delivery;
                });
        WebhookEventFanoutService service = new WebhookEventFanoutService(
                eventRepository,
                subscriptionRepository,
                deliveryRepository,
                idGenerator,
                timeProvider
        );

        List<Long> deliveryIds = service.fanOut(5L);

        assertEquals(List.of(101L), deliveryIds);
        assertEquals(WebhookPublicationStatus.PUBLISHED,
                event.getPublicationStatus());
        assertEquals(1, event.getPublicationAttempts());
        assertEquals(now, event.getPublishedAt());
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(
                WebhookDelivery.class
        );
        verify(deliveryRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().getSubscription().getId());
        assertEquals("dlv_fixed", captor.getValue().getPublicDeliveryId());
    }

    private WebhookSubscription subscription(
            Long id,
            Organization organization
    ) {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(id);
        subscription.setOrganization(organization);
        subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
        return subscription;
    }
}
