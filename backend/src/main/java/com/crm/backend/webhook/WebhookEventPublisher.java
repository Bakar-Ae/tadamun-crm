package com.crm.backend.webhook;

import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
class WebhookEventPublisher {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    private static final int MAX_ID_GENERATION_ATTEMPTS = 5;

    private final WebhookEventRepository eventRepository;
    private final WebhookSubscriptionEventRepository subscriptionEventRepository;
    private final CurrentOrganizationProvider organizationProvider;
    private final SubscriptionTimeProvider timeProvider;
    private final WebhookPublicIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    WebhookEventPublisher(
            WebhookEventRepository eventRepository,
            WebhookSubscriptionEventRepository subscriptionEventRepository,
            CurrentOrganizationProvider organizationProvider,
            SubscriptionTimeProvider timeProvider,
            WebhookPublicIdGenerator idGenerator,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.subscriptionEventRepository = subscriptionEventRepository;
        this.organizationProvider = organizationProvider;
        this.timeProvider = timeProvider;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<WebhookEvent> publish(
            WebhookEventType eventType,
            String aggregateType,
            Long aggregateId,
            Map<String, ?> data
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        if (!subscriptionEventRepository.existsMatchingActiveSubscription(
                organizationId,
                eventType,
                WebhookSubscriptionStatus.ACTIVE
        )) {
            return Optional.empty();
        }

        LocalDateTime occurredAt = timeProvider.now();
        String publicEventId = generateUniqueEventId();
        String payload = serialize(new WebhookEventEnvelope(
                publicEventId,
                SCHEMA_VERSION,
                eventType.getValue(),
                DateTimeFormatter.ISO_INSTANT.format(
                        occurredAt.toInstant(ZoneOffset.UTC)
                ),
                organizationId.toString(),
                Map.copyOf(data)
        ));

        if (payload.getBytes(StandardCharsets.UTF_8).length
                > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "Webhook event payload exceeds 256 KiB"
            );
        }

        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId(publicEventId);
        event.setOrganization(organizationProvider.getOrganizationReference());
        event.setEventType(eventType);
        event.setSchemaVersion(SCHEMA_VERSION);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setPublicationStatus(WebhookPublicationStatus.PENDING);
        event.setNextPublicationAttemptAt(occurredAt);
        event.setOccurredAt(occurredAt);
        return Optional.of(eventRepository.save(event));
    }

    private String generateUniqueEventId() {
        for (int attempt = 0;
             attempt < MAX_ID_GENERATION_ATTEMPTS;
             attempt++) {
            String publicId = idGenerator.eventId();
            if (!eventRepository.existsByPublicEventId(publicId)) {
                return publicId;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique webhook event ID"
        );
    }

    private String serialize(WebhookEventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not serialize webhook event",
                    exception
            );
        }
    }
}
