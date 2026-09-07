package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WebhookDeliveryPreparationService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEndpointValidator endpointValidator;
    private final WebhookSecretEncryptionService encryptionService;
    private final SubscriptionTimeProvider timeProvider;

    public WebhookDeliveryPreparationService(
            WebhookDeliveryRepository deliveryRepository,
            WebhookEndpointValidator endpointValidator,
            WebhookSecretEncryptionService encryptionService,
            SubscriptionTimeProvider timeProvider
    ) {
        this.deliveryRepository = deliveryRepository;
        this.endpointValidator = endpointValidator;
        this.encryptionService = encryptionService;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public PreparedWebhookDelivery prepare(Long deliveryId) {
        return prepare(deliveryId, null);
    }

    @Transactional
    public PreparedWebhookDelivery prepare(
            Long deliveryId,
            String expectedClaimToken
    ) {
        WebhookDelivery delivery = deliveryRepository
                .findForWorkerUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Webhook delivery not found"
                ));
        if (expectedClaimToken != null && !Objects.equals(
                expectedClaimToken,
                delivery.getClaimToken()
        )) {
            throw new IllegalStateException(
                    "Webhook delivery claim is no longer current"
            );
        }
        if (delivery.getStatus() != WebhookDeliveryStatus.PENDING
                && delivery.getStatus()
                != WebhookDeliveryStatus.RETRY_SCHEDULED) {
            throw new IllegalStateException(
                    "Webhook delivery is not ready for an attempt"
            );
        }

        WebhookSubscription subscription = delivery.getSubscription();
        if (subscription.getStatus() != WebhookSubscriptionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Webhook subscription is not active"
            );
        }

        String endpoint = endpointValidator.validateAndNormalize(
                subscription.getEndpointUrl()
        );
        LocalDateTime now = timeProvider.now();
        List<String> secrets = activeSecrets(subscription, now);

        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setStatus(WebhookDeliveryStatus.PROCESSING);
        delivery.setClaimedAt(now);
        deliveryRepository.saveAndFlush(delivery);

        return new PreparedWebhookDelivery(
                delivery.getId(),
                delivery.getAttemptCount(),
                URI.create(endpoint),
                delivery.getPublicDeliveryId(),
                delivery.getEvent().getPublicEventId(),
                delivery.getEvent().getEventType().getValue(),
                delivery.getEvent().getPayload().getBytes(
                        StandardCharsets.UTF_8
                ),
                secrets,
                now.toEpochSecond(ZoneOffset.UTC)
        );
    }

    private List<String> activeSecrets(
            WebhookSubscription subscription,
            LocalDateTime now
    ) {
        List<String> secrets = new ArrayList<>();
        secrets.add(encryptionService.decrypt(encryptedCurrent(subscription)));

        if (subscription.getPreviousSecretExpiresAt() != null
                && subscription.getPreviousSecretExpiresAt().isAfter(now)) {
            secrets.add(encryptionService.decrypt(
                    encryptedPrevious(subscription)
            ));
        }
        return List.copyOf(secrets);
    }

    private EncryptedWebhookSecret encryptedCurrent(
            WebhookSubscription subscription
    ) {
        return new EncryptedWebhookSecret(
                subscription.getCurrentSecretCiphertext(),
                subscription.getCurrentSecretNonce(),
                subscription.getCurrentSecretTag(),
                subscription.getCurrentSecretKeyVersion()
        );
    }

    private EncryptedWebhookSecret encryptedPrevious(
            WebhookSubscription subscription
    ) {
        return new EncryptedWebhookSecret(
                subscription.getPreviousSecretCiphertext(),
                subscription.getPreviousSecretNonce(),
                subscription.getPreviousSecretTag(),
                subscription.getPreviousSecretKeyVersion()
        );
    }
}
