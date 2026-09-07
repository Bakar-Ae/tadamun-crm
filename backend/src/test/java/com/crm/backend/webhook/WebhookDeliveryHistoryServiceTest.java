package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookDeliveryHistoryServiceTest {

    private WebhookDeliveryRepository deliveryRepository;
    private WebhookSubscriptionRepository subscriptionRepository;
    private CurrentOrganizationProvider organizationProvider;
    private SubscriptionFeatureAccessService featureAccessService;
    private SubscriptionTimeProvider timeProvider;
    private WebhookAuditService auditService;
    private WebhookDeliveryHistoryService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(WebhookDeliveryRepository.class);
        WebhookDeliveryAttemptRepository attemptRepository = mock(
                WebhookDeliveryAttemptRepository.class
        );
        subscriptionRepository = mock(WebhookSubscriptionRepository.class);
        organizationProvider = mock(CurrentOrganizationProvider.class);
        featureAccessService = mock(SubscriptionFeatureAccessService.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditService = mock(WebhookAuditService.class);
        now = LocalDateTime.of(2026, 9, 7, 17, 0);
        when(organizationProvider.getOrganizationId()).thenReturn(42L);
        when(timeProvider.now()).thenReturn(now);
        service = new WebhookDeliveryHistoryService(
                deliveryRepository,
                attemptRepository,
                subscriptionRepository,
                new WebhookDeliveryMapper(),
                organizationProvider,
                featureAccessService,
                timeProvider,
                auditService
        );
    }

    @Test
    void shouldReplayFailedDeliveryWithinCurrentOrganization() {
        WebhookDelivery delivery = failedDelivery();
        when(deliveryRepository.findForReplay("dlv_123", 42L, 7L))
                .thenReturn(Optional.of(delivery));
        when(deliveryRepository.saveAndFlush(delivery))
                .thenReturn(delivery);

        service.replay(7L, "dlv_123", 3L);

        assertEquals(WebhookDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(now, delivery.getNextAttemptAt());
        assertNull(delivery.getCompletedAt());
        assertNull(delivery.getLastError());
        verify(featureAccessService).requireFeature(
                42L,
                SubscriptionFeature.WEBHOOKS
        );
        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(
                        WebhookAuditAction.WEBHOOK_DELIVERY_REPLAYED
                ),
                org.mockito.ArgumentMatchers.eq(7L),
                any()
        );
    }

    @Test
    void shouldHideSubscriptionFromAnotherOrganization() {
        when(subscriptionRepository.findByIdAndOrganizationId(7L, 42L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getDeliveries(
                        7L,
                        org.springframework.data.domain.Pageable.unpaged()
                )
        );

        verify(deliveryRepository, never())
                .findByOrganizationIdAndSubscriptionIdOrderByCreatedAtDesc(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void shouldRejectReplayWhileSubscriptionIsPaused() {
        WebhookDelivery delivery = failedDelivery();
        delivery.getSubscription().setStatus(
                WebhookSubscriptionStatus.FAILED
        );
        when(deliveryRepository.findForReplay("dlv_123", 42L, 7L))
                .thenReturn(Optional.of(delivery));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.replay(7L, "dlv_123", 3L)
        );
        verify(deliveryRepository, never()).saveAndFlush(any());
    }

    private WebhookDelivery failedDelivery() {
        Organization organization = new Organization();
        organization.setId(42L);
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(7L);
        subscription.setOrganization(organization);
        subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId("evt_123");
        event.setEventType(WebhookEventType.CUSTOMER_CREATED);
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(9L);
        delivery.setPublicDeliveryId("dlv_123");
        delivery.setOrganization(organization);
        delivery.setSubscription(subscription);
        delivery.setEvent(event);
        delivery.setStatus(WebhookDeliveryStatus.DEAD);
        delivery.setAttemptCount(6);
        delivery.setLastError("old failure");
        delivery.setCompletedAt(now.minusMinutes(1));
        return delivery;
    }
}
