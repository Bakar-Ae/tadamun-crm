package com.crm.backend.subscription.usage;

import com.crm.backend.attachment.AttachmentRepository;
import com.crm.backend.attachment.AttachmentStatus;
import com.crm.backend.notification.NotificationService;
import com.crm.backend.notification.NotificationType;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.subscription.OrganizationSubscription;
import com.crm.backend.subscription.OrganizationSubscriptionRepository;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionPlan;
import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.SubscriptionPlanFeature;
import com.crm.backend.subscription.usage.dto.SubscriptionUsageResponse;
import com.crm.backend.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionUsageServiceTest {

    private OrganizationSubscriptionRepository subscriptionRepository;
    private OrganizationMembershipRepository membershipRepository;
    private AttachmentRepository attachmentRepository;
    private NotificationService notificationService;
    private SubscriptionUsageService usageService;

    @BeforeEach
    void setUp() {
        subscriptionRepository =
                mock(OrganizationSubscriptionRepository.class);
        membershipRepository =
                mock(OrganizationMembershipRepository.class);
        attachmentRepository = mock(AttachmentRepository.class);
        notificationService = mock(NotificationService.class);
        usageService = new SubscriptionUsageService(
                subscriptionRepository,
                membershipRepository,
                attachmentRepository,
                notificationService
        );

        TenantContextHolder.set(new TenantContext(
                10L,
                20L,
                1L,
                RoleName.ADMIN,
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
    void getCurrentUsageShouldCalculateMemberAndStorageMetrics() {
        OrganizationSubscription subscription = subscription(5L, 100L);
        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(membershipRepository.countByOrganizationIdAndStatus(
                10L,
                OrganizationMembershipStatus.ACTIVE
        )).thenReturn(3L);
        when(attachmentRepository.sumSizeBytesByOrganizationIdAndStatus(
                10L,
                AttachmentStatus.ACTIVE
        )).thenReturn(40L);

        SubscriptionUsageResponse response = usageService.getCurrentUsage();

        assertEquals(10L, response.organizationId());
        assertEquals(SubscriptionPlanCode.STARTER, response.planCode());
        assertEquals(3L, response.metrics().get(0).used());
        assertEquals(5L, response.metrics().get(0).limit());
        assertEquals(60, response.metrics().get(0).percentageUsed());
        assertFalse(response.metrics().get(0).warning());
        assertEquals(40L, response.metrics().get(1).used());
        assertEquals(60L, response.metrics().get(1).remaining());
    }

    @Test
    void requireCapacityShouldRejectUsageBeyondMemberLimit() {
        OrganizationSubscription subscription = subscription(5L, 100L);
        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(membershipRepository.countByOrganizationIdAndStatus(
                10L,
                OrganizationMembershipStatus.ACTIVE
        )).thenReturn(5L);

        SubscriptionLimitExceededException exception = assertThrows(
                SubscriptionLimitExceededException.class,
                () -> usageService.requireCapacity(
                        10L,
                        SubscriptionFeature.MEMBERS,
                        1L
                )
        );

        assertEquals(SubscriptionFeature.MEMBERS, exception.getFeature());
        assertEquals(5L, exception.getUsed());
        assertEquals(5L, exception.getLimit());
        verify(notificationService, never()).createNotificationOnce(
                eq(subscription.getOrganization()),
                eq(7L),
                eq("Member usage warning"),
                contains("%"),
                eq(NotificationType.SUBSCRIPTION_USAGE_WARNING),
                contains("MEMBERS")
        );
    }

    @Test
    void requireCapacityShouldWarnAdminsAtProjectedEightyPercent() {
        OrganizationSubscription subscription = subscription(5L, 100L);
        User administrator = new User();
        administrator.setId(7L);
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(administrator);

        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(membershipRepository.countByOrganizationIdAndStatus(
                10L,
                OrganizationMembershipStatus.ACTIVE
        )).thenReturn(3L);
        when(membershipRepository.findNotificationRecipients(
                10L,
                OrganizationMembershipStatus.ACTIVE,
                Set.of(RoleName.OWNER, RoleName.ADMIN)
        )).thenReturn(List.of(membership));

        usageService.requireCapacity(
                10L,
                SubscriptionFeature.MEMBERS,
                1L
        );

        verify(notificationService).createNotificationOnce(
                eq(subscription.getOrganization()),
                eq(7L),
                eq("Member usage warning"),
                contains("80%"),
                eq(NotificationType.SUBSCRIPTION_USAGE_WARNING),
                contains("MEMBERS")
        );
    }

    @Test
    void requireCapacityShouldAllowUnlimitedStorageWithoutWarning() {
        OrganizationSubscription subscription = subscription(5L, null);
        when(subscriptionRepository.findByOrganizationId(10L))
                .thenReturn(Optional.of(subscription));
        when(attachmentRepository.sumSizeBytesByOrganizationIdAndStatus(
                10L,
                AttachmentStatus.ACTIVE
        )).thenReturn(Long.MAX_VALUE - 100L);

        assertDoesNotThrow(() -> usageService.requireCapacity(
                10L,
                SubscriptionFeature.STORAGE_BYTES,
                10L
        ));
        verify(notificationService, never()).createNotificationOnce(
                eq(subscription.getOrganization()),
                eq(7L),
                eq("Storage usage warning"),
                contains("%"),
                eq(NotificationType.SUBSCRIPTION_USAGE_WARNING),
                contains("STORAGE_BYTES")
        );
    }

    private OrganizationSubscription subscription(
            Long memberLimit,
            Long storageLimit
    ) {
        Organization organization = new Organization();
        organization.setId(10L);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(SubscriptionPlanCode.STARTER);
        plan.setFeatures(List.of(
                feature(plan, SubscriptionFeature.MEMBERS, memberLimit),
                feature(plan, SubscriptionFeature.STORAGE_BYTES, storageLimit)
        ));

        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setId(30L);
        subscription.setOrganization(organization);
        subscription.setPlan(plan);
        subscription.setStartedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        return subscription;
    }

    private SubscriptionPlanFeature feature(
            SubscriptionPlan plan,
            SubscriptionFeature feature,
            Long limit
    ) {
        SubscriptionPlanFeature planFeature = new SubscriptionPlanFeature();
        planFeature.setPlan(plan);
        planFeature.setFeature(feature);
        planFeature.setEnabled(true);
        planFeature.setLimitValue(limit);
        return planFeature;
    }
}
