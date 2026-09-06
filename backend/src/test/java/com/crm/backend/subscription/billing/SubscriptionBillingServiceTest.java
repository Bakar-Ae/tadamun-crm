package com.crm.backend.subscription.billing;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.subscription.OrganizationSubscription;
import com.crm.backend.subscription.OrganizationSubscriptionRepository;
import com.crm.backend.subscription.SubscriptionPlan;
import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.SubscriptionStatus;
import com.crm.backend.subscription.billing.dto.BillingSessionResponse;
import com.crm.backend.subscription.billing.dto.CreateBillingCheckoutRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionBillingServiceTest {

    private BillingProviderRegistry providerRegistry;
    private BillingCustomerRepository customerRepository;
    private BillingPlanPriceRepository priceRepository;
    private OrganizationSubscriptionRepository subscriptionRepository;
    private OrganizationRepository organizationRepository;
    private AuditLogService auditLogService;
    private BillingProvider provider;
    private SubscriptionBillingService service;

    @BeforeEach
    void setUp() {
        providerRegistry = mock(BillingProviderRegistry.class);
        customerRepository = mock(BillingCustomerRepository.class);
        priceRepository = mock(BillingPlanPriceRepository.class);
        subscriptionRepository = mock(
                OrganizationSubscriptionRepository.class
        );
        organizationRepository = mock(OrganizationRepository.class);
        auditLogService = mock(AuditLogService.class);
        provider = mock(BillingProvider.class);
        service = new SubscriptionBillingService(
                providerRegistry,
                customerRepository,
                priceRepository,
                subscriptionRepository,
                organizationRepository,
                auditLogService,
                "https://app.tadamun.test/"
        );

        TenantContextHolder.set(new TenantContext(
                10L,
                20L,
                30L,
                RoleName.OWNER,
                DataScope.ALL,
                null,
                Set.of()
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void checkoutShouldUseTenantCustomerAndTrustedUrls() {
        Organization organization = organization();
        OrganizationSubscription subscription = subscription(organization);
        BillingCustomer customer = customer(organization);
        BillingPlanPrice price = price();

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(providerRegistry.require(BillingProviderName.STRIPE))
                .thenReturn(provider);
        when(priceRepository
                .findByPlan_CodeAndProviderAndBillingIntervalAndActiveTrue(
                        SubscriptionPlanCode.BUSINESS,
                        BillingProviderName.STRIPE,
                        BillingInterval.MONTHLY
                )).thenReturn(Optional.of(price));
        when(customerRepository.findByOrganizationIdAndProvider(
                10L,
                BillingProviderName.STRIPE
        )).thenReturn(Optional.of(customer));
        when(provider.createCheckoutSession(any()))
                .thenReturn(new BillingSession(
                        "cs_test_123",
                        URI.create("https://checkout.stripe.test/session")
                ));

        BillingSessionResponse response = service.createCheckout(
                new CreateBillingCheckoutRequest(
                        SubscriptionPlanCode.BUSINESS,
                        BillingInterval.MONTHLY
                ),
                "owner@tadamun.test",
                30L,
                "request-123"
        );

        ArgumentCaptor<BillingCheckoutRequest> requestCaptor =
                ArgumentCaptor.forClass(BillingCheckoutRequest.class);
        verify(provider).createCheckoutSession(requestCaptor.capture());
        BillingCheckoutRequest request = requestCaptor.getValue();

        assertEquals(10L, request.organizationId());
        assertEquals("cus_test_123", request.providerCustomerId());
        assertEquals("price_test_business", request.providerPriceId());
        assertEquals(
                "https://app.tadamun.test/organization?tab=billing&checkout=success",
                request.successUrl().toString()
        );
        assertEquals("checkout-10-request-123", request.idempotencyKey());
        assertEquals("cs_test_123", response.providerSessionId());
        assertEquals(
                "https://checkout.stripe.test/session",
                response.redirectUrl()
        );
        verify(auditLogService).logForOrganization(
                eq(organization),
                eq(30L),
                eq("BILLING_CHECKOUT_CREATED"),
                eq("ORGANIZATION_SUBSCRIPTION"),
                eq(40L),
                any()
        );
    }

    private Organization organization() {
        Organization organization = new Organization();
        organization.setId(10L);
        organization.setName("Tadamun Test");
        return organization;
    }

    private OrganizationSubscription subscription(
            Organization organization
    ) {
        OrganizationSubscription subscription =
                new OrganizationSubscription();
        subscription.setId(40L);
        subscription.setOrganization(organization);
        subscription.setPlan(plan());
        subscription.setStatus(SubscriptionStatus.TRIALING);
        return subscription;
    }

    private BillingCustomer customer(Organization organization) {
        BillingCustomer customer = new BillingCustomer();
        customer.setId(50L);
        customer.setOrganization(organization);
        customer.setProvider(BillingProviderName.STRIPE);
        customer.setProviderCustomerId("cus_test_123");
        return customer;
    }

    private BillingPlanPrice price() {
        BillingPlanPrice price = new BillingPlanPrice();
        price.setId(60L);
        price.setPlan(plan());
        price.setProvider(BillingProviderName.STRIPE);
        price.setProviderPriceId("price_test_business");
        price.setBillingInterval(BillingInterval.MONTHLY);
        price.setCurrency("USD");
        price.setActive(true);
        return price;
    }

    private SubscriptionPlan plan() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(3L);
        plan.setCode(SubscriptionPlanCode.BUSINESS);
        plan.setName("Business");
        plan.setDescription("Business plan");
        plan.setActive(true);
        return plan;
    }
}
