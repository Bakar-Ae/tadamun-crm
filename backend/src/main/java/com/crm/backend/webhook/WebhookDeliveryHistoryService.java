package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.webhook.dto.WebhookDeliveryDetailResponse;
import com.crm.backend.webhook.dto.WebhookDeliveryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WebhookDeliveryHistoryService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDeliveryAttemptRepository attemptRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryMapper mapper;
    private final CurrentOrganizationProvider organizationProvider;
    private final SubscriptionFeatureAccessService featureAccessService;
    private final SubscriptionTimeProvider timeProvider;
    private final WebhookAuditService auditService;

    public WebhookDeliveryHistoryService(
            WebhookDeliveryRepository deliveryRepository,
            WebhookDeliveryAttemptRepository attemptRepository,
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookDeliveryMapper mapper,
            CurrentOrganizationProvider organizationProvider,
            SubscriptionFeatureAccessService featureAccessService,
            SubscriptionTimeProvider timeProvider,
            WebhookAuditService auditService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mapper = mapper;
        this.organizationProvider = organizationProvider;
        this.featureAccessService = featureAccessService;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    public Page<WebhookDeliveryResponse> getDeliveries(
            Long subscriptionId,
            Pageable pageable
    ) {
        Long organizationId = currentOrganizationId();
        requireSubscription(subscriptionId, organizationId);
        return deliveryRepository
                .findByOrganizationIdAndSubscriptionIdOrderByCreatedAtDesc(
                        organizationId,
                        subscriptionId,
                        pageable
                )
                .map(mapper::toResponse);
    }

    public WebhookDeliveryDetailResponse getDelivery(
            Long subscriptionId,
            String publicDeliveryId
    ) {
        Long organizationId = currentOrganizationId();
        requireSubscription(subscriptionId, organizationId);
        WebhookDelivery delivery = deliveryRepository
                .findByPublicDeliveryIdAndOrganizationIdAndSubscriptionId(
                        publicDeliveryId,
                        organizationId,
                        subscriptionId
                )
                .orElseThrow(this::notFound);
        return mapper.toDetail(
                delivery,
                attemptRepository
                        .findByOrganizationIdAndDeliveryIdOrderByAttemptNumberAsc(
                                organizationId,
                                delivery.getId()
                        )
        );
    }

    @Transactional
    public WebhookDeliveryResponse replay(
            Long subscriptionId,
            String publicDeliveryId,
            Long actorUserId
    ) {
        Long organizationId = currentOrganizationId();
        WebhookDelivery delivery = deliveryRepository.findForReplay(
                publicDeliveryId,
                organizationId,
                subscriptionId
        ).orElseThrow(this::notFound);

        if (delivery.getSubscription().getStatus()
                != WebhookSubscriptionStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Enable the webhook subscription before replaying a delivery"
            );
        }
        if (delivery.getStatus()
                != WebhookDeliveryStatus.TERMINAL_FAILURE
                && delivery.getStatus() != WebhookDeliveryStatus.DEAD) {
            throw new IllegalArgumentException(
                    "Only failed webhook deliveries can be replayed"
            );
        }

        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setNextAttemptAt(timeProvider.now());
        delivery.setClaimedAt(null);
        delivery.setClaimToken(null);
        delivery.setLastHttpStatus(null);
        delivery.setLastDurationMs(null);
        delivery.setLastErrorCategory(null);
        delivery.setLastError(null);
        delivery.setLastResponseExcerpt(null);
        delivery.setCompletedAt(null);
        WebhookDelivery saved = deliveryRepository.saveAndFlush(delivery);

        auditService.log(
                organizationId,
                actorUserId,
                WebhookAuditAction.WEBHOOK_DELIVERY_REPLAYED,
                subscriptionId,
                auditService.details(
                        "deliveryId", publicDeliveryId,
                        "eventId", delivery.getEvent().getPublicEventId(),
                        "attemptCount", delivery.getAttemptCount()
                )
        );
        return mapper.toResponse(saved);
    }

    private Long currentOrganizationId() {
        Long organizationId = organizationProvider.getOrganizationId();
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.WEBHOOKS
        );
        return organizationId;
    }

    private void requireSubscription(
            Long subscriptionId,
            Long organizationId
    ) {
        if (subscriptionRepository.findByIdAndOrganizationId(
                subscriptionId,
                organizationId
        ).isEmpty()) {
            throw notFound();
        }
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Webhook delivery not found");
    }
}
