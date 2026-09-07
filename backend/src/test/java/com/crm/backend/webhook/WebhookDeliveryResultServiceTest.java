package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookDeliveryResultServiceTest {

    private WebhookDeliveryRepository deliveryRepository;
    private WebhookDeliveryAttemptRepository attemptRepository;
    private WebhookDeliveryResultService service;
    private WebhookDelivery delivery;
    private LocalDateTime now;
    private WebhookWorkerProperties workerProperties;
    private WebhookFailureNotificationService failureNotificationService;
    private WebhookAuditService auditService;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(WebhookDeliveryRepository.class);
        attemptRepository = mock(WebhookDeliveryAttemptRepository.class);
        SubscriptionTimeProvider timeProvider = mock(
                SubscriptionTimeProvider.class
        );
        now = LocalDateTime.of(2026, 9, 7, 15, 0);
        when(timeProvider.now()).thenReturn(now);
        delivery = processingDelivery();
        when(deliveryRepository.findForWorkerUpdate(20L))
                .thenReturn(Optional.of(delivery));
        workerProperties = new WebhookWorkerProperties();
        failureNotificationService = mock(
                WebhookFailureNotificationService.class
        );
        auditService = mock(WebhookAuditService.class);
        service = new WebhookDeliveryResultService(
                deliveryRepository,
                attemptRepository,
                timeProvider,
                new WebhookRetryPolicy(),
                workerProperties,
                failureNotificationService,
                auditService
        );
    }

    @Test
    void shouldRecordSuccessAndResetSubscriptionFailures() {
        delivery.getSubscription().setConsecutiveFailures(4);

        WebhookDeliveryOutcome outcome = service.record(
                20L,
                1,
                new WebhookDeliveryResult(
                        WebhookDeliveryOutcome.SUCCEEDED,
                        200,
                        12,
                        "accepted",
                        null,
                        null,
                        1_700_000_000L
                )
        );

        assertEquals(WebhookDeliveryOutcome.SUCCEEDED, outcome);
        assertEquals(WebhookDeliveryStatus.SUCCEEDED, delivery.getStatus());
        assertEquals(now, delivery.getCompletedAt());
        assertEquals(0, delivery.getSubscription().getConsecutiveFailures());
        assertEquals(now, delivery.getSubscription().getLastSuccessAt());
        assertNull(delivery.getClaimedAt());
        verify(deliveryRepository).saveAndFlush(delivery);
    }

    @Test
    void shouldDisableSubscriptionOnGoneAndSanitizeAttemptText() {
        String unsafe = "gone\r\n" + "x".repeat(600);

        service.record(
                20L,
                1,
                new WebhookDeliveryResult(
                        WebhookDeliveryOutcome.TERMINAL_FAILURE,
                        410,
                        -1,
                        unsafe,
                        "HTTP_TERMINAL",
                        unsafe,
                        1_700_000_000L
                )
        );

        assertEquals(WebhookDeliveryStatus.TERMINAL_FAILURE,
                delivery.getStatus());
        assertEquals(WebhookSubscriptionStatus.DISABLED,
                delivery.getSubscription().getStatus());
        ArgumentCaptor<WebhookDeliveryAttempt> captor =
                ArgumentCaptor.forClass(WebhookDeliveryAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getDurationMs());
        assertEquals(500, captor.getValue().getErrorMessage().length());
        assertEquals(false, captor.getValue().getErrorMessage().contains("\n"));
    }

    @Test
    void retryableFirstAttemptShouldScheduleOneMinuteBackoff() {
        service.record(
                20L,
                1,
                retryableFailure(500, null)
        );

        assertEquals(WebhookDeliveryStatus.RETRY_SCHEDULED,
                delivery.getStatus());
        assertEquals(now.plusMinutes(1), delivery.getNextAttemptAt());
        assertNull(delivery.getCompletedAt());
    }

    @Test
    void rateLimitShouldHonorRetryAfter() {
        service.record(
                20L,
                1,
                retryableFailure(429, "600")
        );

        assertEquals(now.plusMinutes(10), delivery.getNextAttemptAt());
    }

    @Test
    void retryableSixthAttemptShouldBecomeDead() {
        delivery.setAttemptCount(6);

        service.record(
                20L,
                6,
                retryableFailure(500, null)
        );

        assertEquals(WebhookDeliveryStatus.DEAD, delivery.getStatus());
        assertEquals(now, delivery.getCompletedAt());
        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(
                        WebhookAuditAction.WEBHOOK_DELIVERY_EXHAUSTED
                ),
                org.mockito.ArgumentMatchers.eq(5L),
                any()
        );
    }

    @Test
    void repeatedDeadDeliveryShouldPauseAndNotifySubscription() {
        workerProperties.setFailedSubscriptionThreshold(2);
        delivery.setAttemptCount(6);
        delivery.getSubscription().setConsecutiveFailures(1);

        service.record(
                20L,
                6,
                retryableFailure(500, null)
        );

        assertEquals(WebhookSubscriptionStatus.FAILED,
                delivery.getSubscription().getStatus());
        verify(failureNotificationService).notifySubscriptionFailed(
                delivery.getSubscription()
        );
    }

    private WebhookDeliveryResult retryableFailure(
            int status,
            String retryAfter
    ) {
        return new WebhookDeliveryResult(
                WebhookDeliveryOutcome.RETRYABLE_FAILURE,
                status,
                25,
                "Unavailable",
                "HTTP_RETRYABLE",
                "Webhook endpoint returned HTTP " + status,
                1_700_000_000L,
                retryAfter
        );
    }

    private WebhookDelivery processingDelivery() {
        Organization organization = new Organization();
        organization.setId(42L);
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(5L);
        subscription.setName("Accounting webhook");
        subscription.setOrganization(organization);
        subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
        WebhookDelivery value = new WebhookDelivery();
        value.setId(20L);
        value.setPublicDeliveryId("dlv_test");
        value.setOrganization(organization);
        value.setSubscription(subscription);
        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId("evt_test");
        event.setEventType(WebhookEventType.CUSTOMER_CREATED);
        value.setEvent(event);
        value.setStatus(WebhookDeliveryStatus.PROCESSING);
        value.setAttemptCount(1);
        value.setClaimedAt(now);
        return value;
    }
}
