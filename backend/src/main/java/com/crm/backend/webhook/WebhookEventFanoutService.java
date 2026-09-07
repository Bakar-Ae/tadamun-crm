package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WebhookEventFanoutService {

    private final WebhookEventRepository eventRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookPublicIdGenerator idGenerator;
    private final SubscriptionTimeProvider timeProvider;

    public WebhookEventFanoutService(
            WebhookEventRepository eventRepository,
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookDeliveryRepository deliveryRepository,
            WebhookPublicIdGenerator idGenerator,
            SubscriptionTimeProvider timeProvider
    ) {
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public List<Long> fanOut(Long eventId) {
        return fanOut(eventId, null);
    }

    @Transactional
    public List<Long> fanOut(Long eventId, String expectedClaimToken) {
        WebhookEvent event = eventRepository
                .findForPublicationUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Webhook event not found"
                ));
        if (expectedClaimToken != null && !Objects.equals(
                expectedClaimToken,
                event.getClaimToken()
        )) {
            throw new IllegalStateException(
                    "Webhook event claim is no longer current"
            );
        }
        if (event.getPublicationStatus()
                == WebhookPublicationStatus.PUBLISHED) {
            return List.of();
        }
        if (event.getPublicationStatus()
                == WebhookPublicationStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Webhook event is already being processed"
            );
        }

        event.setPublicationStatus(WebhookPublicationStatus.PROCESSING);
        event.setPublicationAttempts(event.getPublicationAttempts() + 1);
        List<WebhookSubscription> subscriptions = subscriptionRepository
                .findMatchingSubscriptions(
                        event.getOrganization().getId(),
                        WebhookSubscriptionStatus.ACTIVE,
                        event.getEventType()
                );
        LocalDateTime now = timeProvider.now();
        List<Long> createdDeliveryIds = new ArrayList<>();

        for (WebhookSubscription subscription : subscriptions) {
            if (deliveryRepository.existsByEventIdAndSubscriptionId(
                    event.getId(),
                    subscription.getId()
            )) {
                continue;
            }

            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setPublicDeliveryId(idGenerator.deliveryId());
            delivery.setOrganization(event.getOrganization());
            delivery.setEvent(event);
            delivery.setSubscription(subscription);
            delivery.setStatus(WebhookDeliveryStatus.PENDING);
            delivery.setNextAttemptAt(now);
            WebhookDelivery saved = deliveryRepository.save(delivery);
            createdDeliveryIds.add(saved.getId());
        }

        event.setPublicationStatus(WebhookPublicationStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setClaimedAt(null);
        event.setClaimToken(null);
        event.setLastError(null);
        eventRepository.save(event);
        return List.copyOf(createdDeliveryIds);
    }
}
