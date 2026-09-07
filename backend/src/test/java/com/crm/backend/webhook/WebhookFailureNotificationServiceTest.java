package com.crm.backend.webhook;

import com.crm.backend.notification.NotificationService;
import com.crm.backend.notification.NotificationType;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.RoleName;
import com.crm.backend.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookFailureNotificationServiceTest {

    @Test
    void shouldNotifyActiveOrganizationOwnersAndAdminsOnce() {
        OrganizationMembershipRepository membershipRepository = mock(
                OrganizationMembershipRepository.class
        );
        NotificationService notificationService = mock(
                NotificationService.class
        );
        Organization organization = new Organization();
        organization.setId(42L);
        User user = new User();
        user.setId(3L);
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(user);
        when(membershipRepository.findNotificationRecipients(
                42L,
                OrganizationMembershipStatus.ACTIVE,
                Set.of(RoleName.OWNER, RoleName.ADMIN)
        )).thenReturn(List.of(membership));
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(7L);
        subscription.setName("Accounting webhook");
        subscription.setOrganization(organization);
        subscription.setLastFailureAt(
                LocalDateTime.of(2026, 9, 7, 17, 0)
        );

        new WebhookFailureNotificationService(
                membershipRepository,
                notificationService
        ).notifySubscriptionFailed(subscription);

        verify(notificationService).createNotificationOnce(
                eq(organization),
                eq(3L),
                eq("Webhook subscription paused"),
                contains("Accounting webhook"),
                eq(NotificationType.SYSTEM),
                contains("webhook-subscription-failed:7:")
        );
    }
}
