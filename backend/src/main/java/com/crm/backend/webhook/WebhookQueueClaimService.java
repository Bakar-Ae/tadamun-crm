package com.crm.backend.webhook;

import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WebhookQueueClaimService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final WebhookEventRepository eventRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookPublicIdGenerator idGenerator;
    private final SubscriptionTimeProvider timeProvider;
    private final WebhookWorkerProperties properties;
    private final WebhookRetryPolicy retryPolicy;

    public WebhookQueueClaimService(
            WebhookEventRepository eventRepository,
            WebhookDeliveryRepository deliveryRepository,
            WebhookPublicIdGenerator idGenerator,
            SubscriptionTimeProvider timeProvider,
            WebhookWorkerProperties properties,
            WebhookRetryPolicy retryPolicy
    ) {
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.properties = properties;
        this.retryPolicy = retryPolicy;
    }

    @Transactional
    public List<WebhookWorkItem> claimEvents() {
        LocalDateTime now = timeProvider.now();
        List<WebhookEvent> events = eventRepository.lockReadyForPublication(
                now,
                staleBefore(now),
                properties.getBatchSize()
        );
        return events.stream().map(event -> {
            String claimToken = idGenerator.claimToken();
            event.setClaimedAt(now);
            event.setClaimToken(claimToken);
            return new WebhookWorkItem(event.getId(), claimToken);
        }).toList();
    }

    @Transactional
    public List<WebhookWorkItem> claimDeliveries() {
        LocalDateTime now = timeProvider.now();
        List<WebhookDelivery> deliveries =
                deliveryRepository.lockReadyForDelivery(
                        now,
                        staleBefore(now),
                        properties.getBatchSize()
                );
        return deliveries.stream().map(delivery -> {
            String claimToken = idGenerator.claimToken();
            delivery.setClaimedAt(now);
            delivery.setClaimToken(claimToken);
            return new WebhookWorkItem(delivery.getId(), claimToken);
        }).toList();
    }

    @Transactional
    public int recoverStaleDeliveries() {
        LocalDateTime now = timeProvider.now();
        return deliveryRepository.recoverStaleProcessing(
                now,
                staleBefore(now),
                retryPolicy.maximumAttempts()
        );
    }

    @Transactional
    public void recordEventFailure(
            WebhookWorkItem item,
            RuntimeException failure
    ) {
        eventRepository.findForPublicationUpdate(item.id())
                .filter(event -> Objects.equals(
                        item.claimToken(),
                        event.getClaimToken()
                ))
                .ifPresent(event -> {
                    LocalDateTime now = timeProvider.now();
                    int attempts = event.getPublicationAttempts() + 1;
                    event.setPublicationAttempts(attempts);
                    event.setClaimedAt(null);
                    event.setClaimToken(null);
                    event.setLastError(safeMessage(failure));
                    if (attempts >= properties.getEventMaximumAttempts()) {
                        event.setPublicationStatus(
                                WebhookPublicationStatus.FAILED
                        );
                    } else {
                        event.setPublicationStatus(
                                WebhookPublicationStatus.PENDING
                        );
                        event.setNextPublicationAttemptAt(
                                now.plusMinutes(Math.min(attempts, 5))
                        );
                    }
                });
    }

    @Transactional
    public void releaseDeliveryAfterWorkerFailure(
            WebhookWorkItem item,
            RuntimeException failure
    ) {
        deliveryRepository.findForWorkerUpdate(item.id())
                .filter(delivery -> Objects.equals(
                        item.claimToken(),
                        delivery.getClaimToken()
                ))
                .filter(delivery -> !isComplete(delivery.getStatus()))
                .ifPresent(delivery -> {
                    LocalDateTime now = timeProvider.now();
                    delivery.setClaimedAt(null);
                    delivery.setClaimToken(null);
                    delivery.setLastErrorCategory("WORKER_FAILURE");
                    delivery.setLastError(safeMessage(failure));
                    if (delivery.getAttemptCount()
                            >= retryPolicy.maximumAttempts()) {
                        delivery.setStatus(WebhookDeliveryStatus.DEAD);
                        delivery.setCompletedAt(now);
                    } else {
                        delivery.setStatus(
                                WebhookDeliveryStatus.RETRY_SCHEDULED
                        );
                        delivery.setNextAttemptAt(now.plusMinutes(1));
                    }
                });
    }

    private LocalDateTime staleBefore(LocalDateTime now) {
        return now.minusSeconds(properties.getStaleClaimSeconds());
    }

    private boolean isComplete(WebhookDeliveryStatus status) {
        return status == WebhookDeliveryStatus.SUCCEEDED
                || status == WebhookDeliveryStatus.TERMINAL_FAILURE
                || status == WebhookDeliveryStatus.DEAD;
    }

    private String safeMessage(RuntimeException failure) {
        String message = failure.getClass().getSimpleName();
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }
}
