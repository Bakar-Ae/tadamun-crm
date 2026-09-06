package com.crm.backend.subscription.usage;

import com.crm.backend.attachment.AttachmentRepository;
import com.crm.backend.attachment.AttachmentStatus;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.notification.NotificationService;
import com.crm.backend.notification.NotificationType;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.subscription.OrganizationSubscription;
import com.crm.backend.subscription.OrganizationSubscriptionRepository;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionPlanFeature;
import com.crm.backend.subscription.usage.dto.SubscriptionUsageMetricResponse;
import com.crm.backend.subscription.usage.dto.SubscriptionUsageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SubscriptionUsageService {

    private static final int WARNING_PERCENTAGE = 80;
    private static final Set<RoleName> WARNING_RECIPIENT_ROLES = Set.of(
            RoleName.OWNER,
            RoleName.ADMIN
    );

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AttachmentRepository attachmentRepository;
    private final NotificationService notificationService;

    public SubscriptionUsageService(
            OrganizationSubscriptionRepository subscriptionRepository,
            OrganizationMembershipRepository membershipRepository,
            AttachmentRepository attachmentRepository,
            NotificationService notificationService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.membershipRepository = membershipRepository;
        this.attachmentRepository = attachmentRepository;
        this.notificationService = notificationService;
    }

    public SubscriptionUsageResponse getCurrentUsage() {
        Long organizationId = TenantContextHolder.getRequired().organizationId();
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization subscription not found"
                ));

        long members = membershipRepository.countByOrganizationIdAndStatus(
                organizationId,
                OrganizationMembershipStatus.ACTIVE
        );
        long storageBytes =
                attachmentRepository.sumSizeBytesByOrganizationIdAndStatus(
                        organizationId,
                        AttachmentStatus.ACTIVE
                );

        return new SubscriptionUsageResponse(
                organizationId,
                subscription.getPlan().getCode(),
                List.of(
                        metric(subscription, SubscriptionFeature.MEMBERS, members),
                        metric(subscription, SubscriptionFeature.STORAGE_BYTES, storageBytes)
                )
        );
    }

    @Transactional
    public void requireCapacity(
            SubscriptionFeature feature,
            long requestedAmount
    ) {
        requireCapacity(
                TenantContextHolder.getRequired().organizationId(),
                feature,
                requestedAmount
        );
    }

    @Transactional
    public void requireCapacity(
            Long organizationId,
            SubscriptionFeature feature,
            long requestedAmount
    ) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization is required");
        }

        if (requestedAmount <= 0) {
            throw new IllegalArgumentException(
                    "Requested usage must be greater than zero"
            );
        }

        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization subscription not found"
                ));

        long used = currentUsage(organizationId, feature);
        Long limit = resolveLimit(subscription, feature);

        if (limit != null
                && (used >= limit || requestedAmount > limit - used)) {
            throw new SubscriptionLimitExceededException(
                    feature,
                    used,
                    limit
            );
        }

        if (limit != null) {
            notifyIfApproachingLimit(
                    subscription,
                    feature,
                    used + requestedAmount,
                    limit
            );
        }
    }

    private SubscriptionUsageMetricResponse metric(
            OrganizationSubscription subscription,
            SubscriptionFeature feature,
            long used
    ) {
        Long limit = resolveLimit(subscription, feature);

        if (limit == null) {
            return new SubscriptionUsageMetricResponse(
                    feature, used, null, null, null, false, false
            );
        }

        boolean reached = used >= limit;
        long remaining = Math.max(0, limit - used);
        int percentage = limit == 0
                ? 100
                : Math.min(100, (int) Math.ceil(used * 100.0 / limit));

        return new SubscriptionUsageMetricResponse(
                feature,
                used,
                limit,
                remaining,
                percentage,
                percentage >= WARNING_PERCENTAGE && !reached,
                reached
        );
    }

    private long currentUsage(
            Long organizationId,
            SubscriptionFeature feature
    ) {
        return switch (feature) {
            case MEMBERS ->
                    membershipRepository.countByOrganizationIdAndStatus(
                            organizationId,
                            OrganizationMembershipStatus.ACTIVE
                    );
            case STORAGE_BYTES ->
                    attachmentRepository.sumSizeBytesByOrganizationIdAndStatus(
                            organizationId,
                            AttachmentStatus.ACTIVE
                    );
            default -> throw new IllegalArgumentException(
                    "Usage is not metered for " + feature
            );
        };
    }

    private Long resolveLimit(
            OrganizationSubscription subscription,
            SubscriptionFeature feature
    ) {
        SubscriptionPlanFeature planFeature =
                subscription.getPlan().getFeatures().stream()
                        .filter(item -> item.getFeature() == feature)
                        .findFirst()
                        .orElse(null);

        if (planFeature == null) {
            return 0L;
        }

        return effectiveLimit(planFeature);
    }

    private void notifyIfApproachingLimit(
            OrganizationSubscription subscription,
            SubscriptionFeature feature,
            long projectedUsage,
            long limit
    ) {
        if (limit <= 0) {
            return;
        }

        int percentage = Math.min(
                100,
                (int) Math.ceil(projectedUsage * 100.0 / limit)
        );

        if (percentage < WARNING_PERCENTAGE) {
            return;
        }

        String label = switch (feature) {
            case MEMBERS -> "Member";
            case STORAGE_BYTES -> "Storage";
            default -> feature.name();
        };
        String deduplicationKey = "subscription-usage-warning:"
                + feature.name()
                + ":"
                + billingPeriodKey(subscription);
        String message = label + " usage has reached "
                + percentage
                + "% of the current plan limit. Upgrade the subscription "
                + "before additional capacity is required.";

        membershipRepository.findNotificationRecipients(
                subscription.getOrganization().getId(),
                OrganizationMembershipStatus.ACTIVE,
                WARNING_RECIPIENT_ROLES
        ).forEach(membership -> notificationService.createNotificationOnce(
                subscription.getOrganization(),
                membership.getUser().getId(),
                label + " usage warning",
                message,
                NotificationType.SUBSCRIPTION_USAGE_WARNING,
                deduplicationKey
        ));
    }

    private String billingPeriodKey(
            OrganizationSubscription subscription
    ) {
        LocalDateTime periodStart = subscription.getCurrentPeriodStartsAt();

        if (periodStart == null) {
            periodStart = subscription.getStartedAt();
        }

        return periodStart == null
                ? "current"
                : periodStart.toLocalDate().toString();
    }

    private Long effectiveLimit(SubscriptionPlanFeature feature) {
        if (!feature.isEnabled()) {
            return 0L;
        }

        return feature.getLimitValue();
    }
}
