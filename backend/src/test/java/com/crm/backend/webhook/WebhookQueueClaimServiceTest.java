package com.crm.backend.webhook;

import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookQueueClaimServiceTest {

    private WebhookEventRepository eventRepository;
    private WebhookDeliveryRepository deliveryRepository;
    private WebhookPublicIdGenerator idGenerator;
    private WebhookWorkerProperties properties;
    private WebhookQueueClaimService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        eventRepository = mock(WebhookEventRepository.class);
        deliveryRepository = mock(WebhookDeliveryRepository.class);
        idGenerator = mock(WebhookPublicIdGenerator.class);
        SubscriptionTimeProvider timeProvider = mock(
                SubscriptionTimeProvider.class
        );
        properties = new WebhookWorkerProperties();
        now = LocalDateTime.of(2026, 9, 7, 18, 0);
        when(timeProvider.now()).thenReturn(now);
        service = new WebhookQueueClaimService(
                eventRepository,
                deliveryRepository,
                idGenerator,
                timeProvider,
                properties,
                new WebhookRetryPolicy()
        );
    }

    @Test
    void shouldClaimReadyEventsWithUniqueOwnershipToken() {
        WebhookEvent event = new WebhookEvent();
        event.setId(11L);
        when(eventRepository.lockReadyForPublication(
                now,
                now.minusSeconds(120),
                10
        )).thenReturn(List.of(event));
        when(idGenerator.claimToken()).thenReturn("clm_event");

        List<WebhookWorkItem> items = service.claimEvents();

        assertEquals(List.of(new WebhookWorkItem(11L, "clm_event")), items);
        assertEquals(now, event.getClaimedAt());
        assertEquals("clm_event", event.getClaimToken());
    }

    @Test
    void shouldStopRetryingEventFanoutAtConfiguredLimit() {
        properties.setEventMaximumAttempts(2);
        WebhookEvent event = new WebhookEvent();
        event.setId(11L);
        event.setPublicationAttempts(1);
        event.setClaimToken("clm_event");
        when(eventRepository.findForPublicationUpdate(11L))
                .thenReturn(Optional.of(event));

        service.recordEventFailure(
                new WebhookWorkItem(11L, "clm_event"),
                new IllegalStateException("private detail")
        );

        assertEquals(WebhookPublicationStatus.FAILED,
                event.getPublicationStatus());
        assertEquals("IllegalStateException", event.getLastError());
        assertNull(event.getClaimToken());
    }

    @Test
    void shouldRecoverStaleProcessingDeliveries() {
        when(deliveryRepository.recoverStaleProcessing(
                now,
                now.minusSeconds(120),
                6
        )).thenReturn(2);

        assertEquals(2, service.recoverStaleDeliveries());
        verify(deliveryRepository).recoverStaleProcessing(
                now,
                now.minusSeconds(120),
                6
        );
    }
}
