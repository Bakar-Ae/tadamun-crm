package com.crm.backend.webhook;

import com.crm.backend.notification.NotificationService;
import com.crm.backend.notification.NotificationType;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.RoleName;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class WebhookFailureNotificationService {

    private static final Set<RoleName> RECIPIENT_ROLES = Set.of(
            RoleName.OWNER,
            RoleName.ADMIN
    );

    private final OrganizationMembershipRepository membershipRepository;
    private final NotificationService notificationService;

    public WebhookFailureNotificationService(
            OrganizationMembershipRepository membershipRepository,
            NotificationService notificationService
    ) {
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    public void notifySubscriptionFailed(WebhookSubscription subscription) {
        String message = "Webhook subscription '"
                + subscription.getName()
                + "' was paused after repeated delivery failures. Review "
                + "its delivery history before enabling it again.";
        String deduplicationKey = "webhook-subscription-failed:"
                + subscription.getId()
                + ":"
                + subscription.getLastFailureAt();

        membershipRepository.findNotificationRecipients(
                subscription.getOrganization().getId(),
                OrganizationMembershipStatus.ACTIVE,
                RECIPIENT_ROLES
        ).forEach(membership -> notificationService.createNotificationOnce(
                subscription.getOrganization(),
                membership.getUser().getId(),
                "Webhook subscription paused",
                message,
                NotificationType.SYSTEM,
                deduplicationKey
        ));
    }
}
