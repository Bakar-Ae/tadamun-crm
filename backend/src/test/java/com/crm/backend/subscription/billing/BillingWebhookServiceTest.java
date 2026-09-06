package com.crm.backend.subscription.billing;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.subscription.SubscriptionPlan;
import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.SubscriptionService;
import com.crm.backend.subscription.SubscriptionStatus;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingWebhookServiceTest {

    private final LocalDateTime eventTime =
            LocalDateTime.of(2026, 9, 5, 10, 0);
    private BillingProviderRegistry providerRegistry;
    private BillingWebhookEventRepository eventRepository;
    private BillingCustomerRepository customerRepository;
    private BillingPlanPriceRepository priceRepository;
    private OrganizationRepository organizationRepository;
    private SubscriptionService subscriptionService;
    private SubscriptionTimeProvider timeProvider;
    private AuditLogService auditLogService;
    private BillingProvider provider;
    private BillingWebhookService service;

    @BeforeEach
    void setUp() {
        providerRegistry = mock(BillingProviderRegistry.class);
        eventRepository = mock(BillingWebhookEventRepository.class);
        customerRepository = mock(BillingCustomerRepository.class);
        priceRepository = mock(BillingPlanPriceRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        subscriptionService = mock(SubscriptionService.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditLogService = mock(AuditLogService.class);
        provider = mock(BillingProvider.class);
        service = new BillingWebhookService(
                providerRegistry,
                eventRepository,
                customerRepository,
                priceRepository,
                organizationRepository,
                subscriptionService,
                timeProvider,
                auditLogService
        );

        when(providerRegistry.require(BillingProviderName.STRIPE))
                .thenReturn(provider);
        when(timeProvider.now()).thenReturn(eventTime.plusMinutes(1));
        when(eventRepository.saveAndFlush(any(BillingWebhookEvent.class)))
                .thenAnswer(invocation -> {
                    BillingWebhookEvent event = invocation.getArgument(0);
                    if (event.getId() == null) {
                        event.setId(100L);
                    }
                    return event;
                });
    }

    @Test
    void verifiedSubscriptionEventShouldSynchronizeOnce() {
        BillingWebhookNotification notification = activeNotification();
        Organization organization = organization();
        BillingCustomer customer = customer(organization);
        BillingPlanPrice price = price();

        when(provider.verifyWebhook("payload", "signature"))
                .thenReturn(notification);
        when(eventRepository.findByProviderAndProviderEventId(
                BillingProviderName.STRIPE,
                "evt_123"
        )).thenReturn(Optional.empty());
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(customerRepository.findByOrganizationIdAndProvider(
                10L,
                BillingProviderName.STRIPE
        )).thenReturn(Optional.of(customer));
        when(priceRepository.findByProviderAndProviderPriceId(
                BillingProviderName.STRIPE,
                "price_business"
        )).thenReturn(Optional.of(price));

        BillingWebhookProcessingResult result =
                service.processStripeWebhook("payload", "signature");

        assertTrue(result.received());
        assertFalse(result.duplicate());
        assertEquals(
                BillingWebhookProcessingStatus.PROCESSED,
                result.status()
        );
        verify(subscriptionService).synchronizeProviderSubscription(
                10L,
                BillingProviderName.STRIPE,
                "sub_123",
                SubscriptionPlanCode.BUSINESS,
                SubscriptionStatus.ACTIVE,
                eventTime,
                eventTime.minusDays(1),
                eventTime.plusMonths(1),
                false
        );
    }

    @Test
    void processedEventShouldBeAcknowledgedAsDuplicate() {
        BillingWebhookEvent existing = new BillingWebhookEvent();
        existing.setId(100L);
        existing.setProcessingStatus(
                BillingWebhookProcessingStatus.PROCESSED
        );

        when(provider.verifyWebhook("payload", "signature"))
                .thenReturn(activeNotification());
        when(eventRepository.findByProviderAndProviderEventId(
                BillingProviderName.STRIPE,
                "evt_123"
        )).thenReturn(Optional.of(existing));

        BillingWebhookProcessingResult result =
                service.processStripeWebhook("payload", "signature");

        assertTrue(result.duplicate());
        assertEquals(
                BillingWebhookProcessingStatus.PROCESSED,
                result.status()
        );
        verify(subscriptionService, never())
                .synchronizeProviderSubscription(
                        any(), any(), any(), any(), any(), any(),
                        any(), any(), eq(false)
                );
    }

    private BillingWebhookNotification activeNotification() {
        return new BillingWebhookNotification(
                "evt_123",
                "customer.subscription.updated",
                BillingWebhookType.SUBSCRIPTION_CHANGED,
                10L,
                "cus_123",
                "sub_123",
                "price_business",
                SubscriptionPlanCode.BUSINESS,
                BillingProviderSubscriptionStatus.ACTIVE,
                eventTime,
                eventTime.minusDays(1),
                eventTime.plusMonths(1),
                false
        );
    }

    private Organization organization() {
        Organization organization = new Organization();
        organization.setId(10L);
        organization.setName("Tadamun Test");
        return organization;
    }

    private BillingCustomer customer(Organization organization) {
        BillingCustomer customer = new BillingCustomer();
        customer.setId(20L);
        customer.setOrganization(organization);
        customer.setProvider(BillingProviderName.STRIPE);
        customer.setProviderCustomerId("cus_123");
        return customer;
    }

    private BillingPlanPrice price() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(3L);
        plan.setCode(SubscriptionPlanCode.BUSINESS);
        plan.setName("Business");
        plan.setDescription("Business plan");
        plan.setActive(true);

        BillingPlanPrice price = new BillingPlanPrice();
        price.setId(30L);
        price.setPlan(plan);
        price.setProvider(BillingProviderName.STRIPE);
        price.setProviderPriceId("price_business");
        return price;
    }
}
