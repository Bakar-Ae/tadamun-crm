package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WebhookDeliveryResultService {

    private static final int MAX_RESPONSE_CHARS = 4096;
    private static final int MAX_ERROR_CHARS = 500;

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDeliveryAttemptRepository attemptRepository;
    private final SubscriptionTimeProvider timeProvider;
    private final WebhookRetryPolicy retryPolicy;
    private final WebhookWorkerProperties workerProperties;
    private final WebhookFailureNotificationService failureNotificationService;
    private final WebhookAuditService auditService;

    public WebhookDeliveryResultService(
            WebhookDeliveryRepository deliveryRepository,
            WebhookDeliveryAttemptRepository attemptRepository,
            SubscriptionTimeProvider timeProvider,
            WebhookRetryPolicy retryPolicy,
            WebhookWorkerProperties workerProperties,
            WebhookFailureNotificationService failureNotificationService,
            WebhookAuditService auditService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.timeProvider = timeProvider;
        this.retryPolicy = retryPolicy;
        this.workerProperties = workerProperties;
        this.failureNotificationService = failureNotificationService;
        this.auditService = auditService;
    }

    @Transactional
    public WebhookDeliveryOutcome record(
            Long deliveryId,
            int attemptNumber,
            WebhookDeliveryResult result
    ) {
        WebhookDelivery delivery = deliveryRepository
                .findForWorkerUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Webhook delivery not found"
                ));
        if (delivery.getStatus() != WebhookDeliveryStatus.PROCESSING
                || delivery.getAttemptCount() != attemptNumber) {
            throw new IllegalStateException(
                    "Webhook delivery attempt is no longer current"
            );
        }

        LocalDateTime now = timeProvider.now();
        WebhookDeliveryAttempt attempt = new WebhookDeliveryAttempt();
        attempt.setOrganization(delivery.getOrganization());
        attempt.setDelivery(delivery);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setOutcome(result.outcome());
        attempt.setRequestTimestamp(result.requestTimestamp());
        attempt.setHttpStatus(result.httpStatus());
        attempt.setDurationMs(Math.max(0, result.durationMs()));
        attempt.setResponseExcerpt(sanitize(
                result.responseExcerpt(),
                MAX_RESPONSE_CHARS
        ));
        attempt.setErrorCategory(sanitize(
                result.errorCategory(),
                60
        ));
        attempt.setErrorMessage(sanitize(
                result.errorMessage(),
                MAX_ERROR_CHARS
        ));
        attemptRepository.save(attempt);

        delivery.setLastHttpStatus(result.httpStatus());
        delivery.setLastDurationMs(Math.max(0, result.durationMs()));
        delivery.setLastResponseExcerpt(attempt.getResponseExcerpt());
        delivery.setLastErrorCategory(attempt.getErrorCategory());
        delivery.setLastError(attempt.getErrorMessage());
        delivery.setClaimedAt(null);
        delivery.setClaimToken(null);

        WebhookSubscription subscription = delivery.getSubscription();
        boolean subscriptionFailed = false;
        boolean deliveryExhausted = false;
        if (result.outcome() == WebhookDeliveryOutcome.SUCCEEDED) {
            delivery.setStatus(WebhookDeliveryStatus.SUCCEEDED);
            delivery.setCompletedAt(now);
            subscription.setConsecutiveFailures(0);
            subscription.setLastSuccessAt(now);
        } else {
            subscription.setConsecutiveFailures(
                    subscription.getConsecutiveFailures() + 1
            );
            subscription.setLastFailureAt(now);

            if (result.outcome()
                    == WebhookDeliveryOutcome.RETRYABLE_FAILURE) {
                String retryAfter = Integer.valueOf(429).equals(
                        result.httpStatus()
                ) ? result.retryAfter() : null;
                LocalDateTime nextAttemptAt = retryPolicy.nextAttemptAt(
                        attemptNumber,
                        now,
                        retryAfter
                ).orElse(null);
                if (nextAttemptAt == null) {
                    delivery.setStatus(WebhookDeliveryStatus.DEAD);
                    delivery.setCompletedAt(now);
                    deliveryExhausted = true;
                } else {
                    delivery.setStatus(
                            WebhookDeliveryStatus.RETRY_SCHEDULED
                    );
                    delivery.setNextAttemptAt(nextAttemptAt);
                }
            } else {
                delivery.setStatus(WebhookDeliveryStatus.TERMINAL_FAILURE);
                delivery.setCompletedAt(now);
            }

            if (Integer.valueOf(410).equals(result.httpStatus())) {
                subscription.setStatus(WebhookSubscriptionStatus.DISABLED);
            }

            if (delivery.getStatus() == WebhookDeliveryStatus.DEAD
                    && subscription.getStatus()
                    == WebhookSubscriptionStatus.ACTIVE
                    && subscription.getConsecutiveFailures()
                    >= workerProperties.getFailedSubscriptionThreshold()) {
                subscription.setStatus(WebhookSubscriptionStatus.FAILED);
                subscriptionFailed = true;
            }
        }

        deliveryRepository.saveAndFlush(delivery);
        if (deliveryExhausted) {
            auditService.log(
                    delivery.getOrganization().getId(),
                    null,
                    WebhookAuditAction.WEBHOOK_DELIVERY_EXHAUSTED,
                    subscription.getId(),
                    auditService.details(
                            "deliveryId", delivery.getPublicDeliveryId(),
                            "eventId", delivery.getEvent().getPublicEventId(),
                            "eventType",
                            delivery.getEvent().getEventType().getValue(),
                            "attemptCount", delivery.getAttemptCount(),
                            "httpStatus", delivery.getLastHttpStatus(),
                            "errorCategory", delivery.getLastErrorCategory()
                    )
            );
        }
        if (subscriptionFailed) {
            failureNotificationService.notifySubscriptionFailed(
                    subscription
            );
        }
        return result.outcome();
    }

    private String sanitize(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        return sanitized.length() <= maximumLength
                ? sanitized
                : sanitized.substring(0, maximumLength);
    }
}
