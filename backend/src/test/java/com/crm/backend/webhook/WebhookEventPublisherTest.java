package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookEventPublisherTest {

    private WebhookEventRepository eventRepository;
    private WebhookSubscriptionEventRepository subscriptionEventRepository;
    private WebhookEventPublisher publisher;

    @BeforeEach
    void setUp() {
        eventRepository = mock(WebhookEventRepository.class);
        subscriptionEventRepository = mock(
                WebhookSubscriptionEventRepository.class
        );
        CurrentOrganizationProvider organizationProvider = mock(
                CurrentOrganizationProvider.class
        );
        SubscriptionTimeProvider timeProvider = mock(
                SubscriptionTimeProvider.class
        );
        WebhookPublicIdGenerator idGenerator = mock(
                WebhookPublicIdGenerator.class
        );
        Organization organization = new Organization();
        organization.setId(42L);
        when(organizationProvider.getOrganizationId()).thenReturn(42L);
        when(organizationProvider.getOrganizationReference())
                .thenReturn(organization);
        when(timeProvider.now()).thenReturn(
                LocalDateTime.of(2026, 9, 7, 12, 30)
        );
        when(idGenerator.eventId()).thenReturn("evt_fixed");
        when(eventRepository.existsByPublicEventId("evt_fixed"))
                .thenReturn(false);
        when(eventRepository.save(any(WebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        publisher = new WebhookEventPublisher(
                eventRepository,
                subscriptionEventRepository,
                organizationProvider,
                timeProvider,
                idGenerator,
                new ObjectMapper()
        );
    }

    @Test
    void shouldPersistStableOrganizationScopedEnvelope() throws Exception {
        when(subscriptionEventRepository.existsMatchingActiveSubscription(
                42L,
                WebhookEventType.CUSTOMER_CREATED,
                WebhookSubscriptionStatus.ACTIVE
        )).thenReturn(true);

        Optional<WebhookEvent> result = publisher.publish(
                WebhookEventType.CUSTOMER_CREATED,
                "CUSTOMER",
                9L,
                Map.of("customer", Map.of("id", 9L, "name", "Acme"))
        );

        assertTrue(result.isPresent());
        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(
                WebhookEvent.class
        );
        verify(eventRepository).save(captor.capture());
        WebhookEvent stored = captor.getValue();
        JsonNode payload = new ObjectMapper().readTree(stored.getPayload());
        assertEquals("evt_fixed", payload.get("id").asText());
        assertEquals(1, payload.get("schemaVersion").asInt());
        assertEquals("customer.created", payload.get("type").asText());
        assertEquals("42", payload.get("organizationId").asText());
        assertEquals("Acme", payload.get("data")
                .get("customer").get("name").asText());
        assertEquals(WebhookPublicationStatus.PENDING,
                stored.getPublicationStatus());
    }

    @Test
    void shouldSkipOutboxWriteWhenNoActiveSubscriberMatches() {
        Optional<WebhookEvent> result = publisher.publish(
                WebhookEventType.TASK_COMPLETED,
                "TASK",
                5L,
                Map.of("task", Map.of("id", 5L))
        );

        assertTrue(result.isEmpty());
        verify(eventRepository, never()).save(any());
    }
}
