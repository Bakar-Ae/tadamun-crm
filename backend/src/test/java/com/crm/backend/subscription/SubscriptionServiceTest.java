package com.crm.backend.subscription;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.subscription.dto.OrganizationSubscriptionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    private final LocalDateTime now =
            LocalDateTime.of(2026, 9, 5, 10, 0);
    private SubscriptionPlanRepository planRepository;
    private OrganizationSubscriptionRepository subscriptionRepository;
    private SubscriptionTimeProvider timeProvider;
    private AuditLogService auditLogService;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        planRepository = mock(SubscriptionPlanRepository.class);
        subscriptionRepository = mock(
                OrganizationSubscriptionRepository.class
        );
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditLogService = mock(AuditLogService.class);
        when(timeProvider.now()).thenReturn(now);

        subscriptionService = new SubscriptionService(
                planRepository,
                subscriptionRepository,
                new SubscriptionMapper(),
                new SubscriptionLifecyclePolicy(),
                timeProvider,
                auditLogService,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void currentSubscriptionShouldUseTenantAndResolveExpiredTrial() {
        setTenantContext(10L);
        OrganizationSubscription subscription = subscription(
                10L,
                plan(SubscriptionPlanCode.STARTER, 14, 7),
                SubscriptionStatus.TRIALING
        );
        subscription.setTrialEndsAt(now.minusMinutes(1));

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));

        OrganizationSubscriptionResponse response =
                subscriptionService.getCurrentSubscription();

        assertEquals(10L, response.organizationId());
        assertEquals(SubscriptionStatus.EXPIRED, response.status());
        assertFalse(response.accessAllowed());
    }

    @Test
    void startTrialShouldCreateStarterTrialAndAuditIt() {
        Organization organization = organization(10L);
        SubscriptionPlan starter = plan(
                SubscriptionPlanCode.STARTER,
                14,
                7
        );

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.empty());
        when(planRepository.findByCodeAndActiveTrue(
                SubscriptionPlanCode.STARTER
        )).thenReturn(Optional.of(starter));
        when(subscriptionRepository.saveAndFlush(
                any(OrganizationSubscription.class)
        )).thenAnswer(invocation -> {
            OrganizationSubscription saved = invocation.getArgument(0);
            saved.setId(50L);
            saved.setVersion(0L);
            return saved;
        });

        OrganizationSubscriptionResponse response = subscriptionService
                .startTrialForOrganization(organization, 1L);

        assertEquals(SubscriptionStatus.TRIALING, response.status());
        assertEquals(now.plusDays(14), response.trialEndsAt());
        assertTrue(response.accessAllowed());
        verify(auditLogService).logForOrganization(
                eq(organization),
                eq(1L),
                eq("SUBSCRIPTION_TRIAL_STARTED"),
                eq("ORGANIZATION_SUBSCRIPTION"),
                eq(50L),
                anyString()
        );
    }

    @Test
    void existingSubscriptionShouldMakeTrialProvisioningIdempotent() {
        Organization organization = organization(10L);
        OrganizationSubscription existing = subscription(
                10L,
                plan(SubscriptionPlanCode.STARTER, 14, 7),
                SubscriptionStatus.TRIALING
        );
        existing.setTrialEndsAt(now.plusDays(10));

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(existing));

        OrganizationSubscriptionResponse response = subscriptionService
                .startTrialForOrganization(organization, 1L);

        assertEquals(50L, response.id());
        verify(subscriptionRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).logForOrganization(
                any(), any(), anyString(), anyString(), any(), anyString()
        );
    }

    @Test
    void activationShouldChangePlanAndCreateAuditEvents() {
        SubscriptionPlan starter = plan(
                SubscriptionPlanCode.STARTER,
                14,
                7
        );
        SubscriptionPlan business = plan(
                SubscriptionPlanCode.BUSINESS,
                14,
                14
        );
        OrganizationSubscription subscription = subscription(
                10L,
                starter,
                SubscriptionStatus.TRIALING
        );

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(planRepository.findByCodeAndActiveTrue(
                SubscriptionPlanCode.BUSINESS
        )).thenReturn(Optional.of(business));
        when(subscriptionRepository.saveAndFlush(subscription))
                .thenReturn(subscription);

        OrganizationSubscriptionResponse response = subscriptionService
                .activateSubscription(
                        10L,
                        SubscriptionPlanCode.BUSINESS,
                        now,
                        now.plusMonths(1),
                        1L
                );

        assertEquals(SubscriptionStatus.ACTIVE, response.status());
        assertEquals(SubscriptionPlanCode.BUSINESS, response.plan().code());
        verify(auditLogService).logForOrganization(
                any(Organization.class),
                eq(1L),
                eq("SUBSCRIPTION_PLAN_CHANGED"),
                eq("ORGANIZATION_SUBSCRIPTION"),
                eq(50L),
                anyString()
        );
        verify(auditLogService).logForOrganization(
                any(Organization.class),
                eq(1L),
                eq("SUBSCRIPTION_ACTIVATED"),
                eq("ORGANIZATION_SUBSCRIPTION"),
                eq(50L),
                anyString()
        );
    }

    @Test
    void gracePeriodShouldUsePlanDuration() {
        OrganizationSubscription subscription = subscription(
                10L,
                plan(SubscriptionPlanCode.PROFESSIONAL, 14, 7),
                SubscriptionStatus.PAST_DUE
        );

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.saveAndFlush(subscription))
                .thenReturn(subscription);

        OrganizationSubscriptionResponse response = subscriptionService
                .startGracePeriod(10L, 1L);

        assertEquals(SubscriptionStatus.GRACE_PERIOD, response.status());
        assertEquals(now.plusDays(7), response.gracePeriodEndsAt());
    }

    @Test
    void cancellationScheduleShouldRequireFutureActivePeriod() {
        OrganizationSubscription subscription = subscription(
                10L,
                plan(SubscriptionPlanCode.STARTER, 14, 7),
                SubscriptionStatus.TRIALING
        );

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.scheduleCancellation(10L, 1L)
        );

        assertEquals(
                "Only an active subscription with a future period end can be canceled later",
                exception.getMessage()
        );
    }

    private void setTenantContext(Long organizationId) {
        TenantContextHolder.set(new TenantContext(
                organizationId,
                100L,
                1L,
                RoleName.OWNER,
                DataScope.ALL,
                null,
                Set.of()
        ));
    }

    private Organization organization(Long id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Tadamun Business");
        return organization;
    }

    private SubscriptionPlan plan(
            SubscriptionPlanCode code,
            int trialDays,
            int gracePeriodDays
    ) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId((long) code.ordinal() + 1);
        plan.setCode(code);
        plan.setName(code.name());
        plan.setDescription(code.name() + " plan");
        plan.setTrialDays(trialDays);
        plan.setGracePeriodDays(gracePeriodDays);
        plan.setActive(true);
        return plan;
    }

    private OrganizationSubscription subscription(
            Long organizationId,
            SubscriptionPlan plan,
            SubscriptionStatus status
    ) {
        OrganizationSubscription subscription =
                new OrganizationSubscription();
        subscription.setId(50L);
        subscription.setOrganization(organization(organizationId));
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setStartedAt(now.minusDays(1));
        subscription.setVersion(0L);
        return subscription;
    }
}
