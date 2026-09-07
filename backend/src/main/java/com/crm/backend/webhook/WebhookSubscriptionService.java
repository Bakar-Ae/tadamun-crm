package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.webhook.dto.CreateWebhookSubscriptionRequest;
import com.crm.backend.webhook.dto.UpdateWebhookSubscriptionRequest;
import com.crm.backend.webhook.dto.WebhookSubscriptionResponse;
import com.crm.backend.webhook.dto.WebhookSubscriptionSecretResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WebhookSubscriptionService {

    private static final int MAX_SUBSCRIPTIONS_PER_ORGANIZATION = 20;
    private static final long PREVIOUS_SECRET_GRACE_HOURS = 24;

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookSubscriptionEventRepository eventRepository;
    private final WebhookSecretEncryptionService secretEncryptionService;
    private final WebhookEndpointValidator endpointValidator;
    private final WebhookSubscriptionMapper mapper;
    private final CurrentOrganizationProvider organizationProvider;
    private final UserRepository userRepository;
    private final SubscriptionFeatureAccessService featureAccessService;
    private final SubscriptionTimeProvider timeProvider;
    private final WebhookAuditService auditService;

    public WebhookSubscriptionService(
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookSubscriptionEventRepository eventRepository,
            WebhookSecretEncryptionService secretEncryptionService,
            WebhookEndpointValidator endpointValidator,
            WebhookSubscriptionMapper mapper,
            CurrentOrganizationProvider organizationProvider,
            UserRepository userRepository,
            SubscriptionFeatureAccessService featureAccessService,
            SubscriptionTimeProvider timeProvider,
            WebhookAuditService auditService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.secretEncryptionService = secretEncryptionService;
        this.endpointValidator = endpointValidator;
        this.mapper = mapper;
        this.organizationProvider = organizationProvider;
        this.userRepository = userRepository;
        this.featureAccessService = featureAccessService;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    public Page<WebhookSubscriptionResponse> getSubscriptions(
            Pageable pageable
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        Page<WebhookSubscription> subscriptions = subscriptionRepository
                .findByOrganizationId(organizationId, pageable);
        Map<Long, List<WebhookSubscriptionEvent>> eventsBySubscription =
                loadEventsBySubscription(
                        organizationId,
                        subscriptions.getContent().stream()
                                .map(WebhookSubscription::getId)
                                .toList()
                );
        return subscriptions.map(subscription -> mapper.toResponse(
                subscription,
                eventsBySubscription.getOrDefault(
                        subscription.getId(),
                        List.of()
                )
        ));
    }

    public WebhookSubscriptionResponse getSubscription(Long id) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WebhookSubscription subscription = subscriptionRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(this::notFound);
        return toResponse(subscription, organizationId);
    }

    @Transactional
    public WebhookSubscriptionSecretResponse createSubscription(
            CreateWebhookSubscriptionRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        ensureCapacity(organizationId);

        Set<WebhookEventType> eventTypes = parseEventTypes(
                request.eventTypes()
        );
        GeneratedWebhookSecret generated = secretEncryptionService.generate();
        User actor = userRepository.getReferenceById(actorUserId);

        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setOrganization(
                organizationProvider.getOrganizationReference()
        );
        subscription.setName(request.name().trim());
        subscription.setEndpointUrl(
                endpointValidator.validateAndNormalize(request.endpointUrl())
        );
        subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
        subscription.setCreatedByUser(actor);
        applyCurrentSecret(subscription, generated);

        WebhookSubscription saved = subscriptionRepository.saveAndFlush(
                subscription
        );
        List<WebhookSubscriptionEvent> events = replaceEvents(
                saved,
                organizationId,
                eventTypes,
                false
        );
        auditService.log(
                organizationId,
                actorUserId,
                WebhookAuditAction.WEBHOOK_SUBSCRIPTION_CREATED,
                saved.getId(),
                auditService.details(
                        "name", saved.getName(),
                        "eventTypes", eventTypeValues(eventTypes),
                        "status", saved.getStatus().name()
                )
        );

        return new WebhookSubscriptionSecretResponse(
                generated.rawSecret(),
                mapper.toResponse(saved, events)
        );
    }

    @Transactional
    public WebhookSubscriptionResponse updateSubscription(
            Long id,
            UpdateWebhookSubscriptionRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WebhookSubscription subscription = findForUpdate(id, organizationId);
        requireNotRevoked(subscription);
        Set<WebhookEventType> eventTypes = parseEventTypes(
                request.eventTypes()
        );

        subscription.setName(request.name().trim());
        subscription.setEndpointUrl(
                endpointValidator.validateAndNormalize(request.endpointUrl())
        );
        subscription.setUpdatedByUser(
                userRepository.getReferenceById(actorUserId)
        );
        WebhookSubscription saved = subscriptionRepository.saveAndFlush(
                subscription
        );
        List<WebhookSubscriptionEvent> events = replaceEvents(
                saved,
                organizationId,
                eventTypes,
                true
        );
        auditService.log(
                organizationId,
                actorUserId,
                WebhookAuditAction.WEBHOOK_SUBSCRIPTION_UPDATED,
                saved.getId(),
                auditService.details(
                        "name", saved.getName(),
                        "eventTypes", eventTypeValues(eventTypes),
                        "status", saved.getStatus().name()
                )
        );
        return mapper.toResponse(saved, events);
    }

    @Transactional
    public WebhookSubscriptionResponse enableSubscription(
            Long id,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WebhookSubscription subscription = findForUpdate(id, organizationId);
        requireNotRevoked(subscription);

        if (subscription.getStatus() != WebhookSubscriptionStatus.ACTIVE) {
            subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
            subscription.setConsecutiveFailures(0);
            subscription.setUpdatedByUser(
                    userRepository.getReferenceById(actorUserId)
            );
            subscriptionRepository.saveAndFlush(subscription);
            auditService.log(
                    organizationId,
                    actorUserId,
                    WebhookAuditAction.WEBHOOK_SUBSCRIPTION_ENABLED,
                    subscription.getId(),
                    auditService.details("name", subscription.getName())
            );
        }
        return toResponse(subscription, organizationId);
    }

    @Transactional
    public WebhookSubscriptionResponse disableSubscription(
            Long id,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WebhookSubscription subscription = findForUpdate(id, organizationId);
        requireNotRevoked(subscription);

        if (subscription.getStatus() != WebhookSubscriptionStatus.DISABLED) {
            subscription.setStatus(WebhookSubscriptionStatus.DISABLED);
            subscription.setUpdatedByUser(
                    userRepository.getReferenceById(actorUserId)
            );
            subscriptionRepository.saveAndFlush(subscription);
            auditService.log(
                    organizationId,
                    actorUserId,
                    WebhookAuditAction.WEBHOOK_SUBSCRIPTION_DISABLED,
                    subscription.getId(),
                    auditService.details("name", subscription.getName())
            );
        }
        return toResponse(subscription, organizationId);
    }

    @Transactional
    public WebhookSubscriptionSecretResponse rotateSecret(
            Long id,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WebhookSubscription subscription = findForUpdate(id, organizationId);
        requireNotRevoked(subscription);
        LocalDateTime now = timeProvider.now();
        GeneratedWebhookSecret generated = secretEncryptionService.generate();

        moveCurrentSecretToPrevious(subscription, now);
        applyCurrentSecret(subscription, generated);
        subscription.setUpdatedByUser(
                userRepository.getReferenceById(actorUserId)
        );
        WebhookSubscription saved = subscriptionRepository.saveAndFlush(
                subscription
        );
        auditService.log(
                organizationId,
                actorUserId,
                WebhookAuditAction.WEBHOOK_SECRET_ROTATED,
                saved.getId(),
                auditService.details(
                        "name", saved.getName(),
                        "previousSecretExpiresAt",
                        saved.getPreviousSecretExpiresAt()
                )
        );
        return new WebhookSubscriptionSecretResponse(
                generated.rawSecret(),
                toResponse(saved, organizationId)
        );
    }

    @Transactional
    public WebhookSubscriptionResponse revokeSubscription(
            Long id,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WebhookSubscription subscription = findForUpdate(id, organizationId);

        if (subscription.getStatus() == WebhookSubscriptionStatus.REVOKED) {
            return toResponse(subscription, organizationId);
        }

        User actor = userRepository.getReferenceById(actorUserId);
        subscription.setStatus(WebhookSubscriptionStatus.REVOKED);
        subscription.setCurrentSecretCiphertext(null);
        subscription.setCurrentSecretNonce(null);
        subscription.setCurrentSecretTag(null);
        subscription.setCurrentSecretKeyVersion(null);
        clearPreviousSecret(subscription);
        subscription.setRevokedByUser(actor);
        subscription.setRevokedAt(timeProvider.now());
        subscription.setUpdatedByUser(actor);
        WebhookSubscription saved = subscriptionRepository.saveAndFlush(
                subscription
        );
        auditService.log(
                organizationId,
                actorUserId,
                WebhookAuditAction.WEBHOOK_SUBSCRIPTION_REVOKED,
                saved.getId(),
                auditService.details("name", saved.getName())
        );
        return toResponse(saved, organizationId);
    }

    private WebhookSubscription findForUpdate(
            Long id,
            Long organizationId
    ) {
        return subscriptionRepository.findForUpdate(id, organizationId)
                .orElseThrow(this::notFound);
    }

    private WebhookSubscriptionResponse toResponse(
            WebhookSubscription subscription,
            Long organizationId
    ) {
        return mapper.toResponse(
                subscription,
                eventRepository.findByOrganizationIdAndIdSubscriptionId(
                        organizationId,
                        subscription.getId()
                )
        );
    }

    private Map<Long, List<WebhookSubscriptionEvent>>
    loadEventsBySubscription(
            Long organizationId,
            Collection<Long> subscriptionIds
    ) {
        if (subscriptionIds.isEmpty()) {
            return Map.of();
        }
        return eventRepository
                .findByOrganizationIdAndIdSubscriptionIdIn(
                        organizationId,
                        subscriptionIds
                )
                .stream()
                .collect(Collectors.groupingBy(
                        event -> event.getId().getSubscriptionId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<WebhookSubscriptionEvent> replaceEvents(
            WebhookSubscription subscription,
            Long organizationId,
            Set<WebhookEventType> eventTypes,
            boolean deleteExisting
    ) {
        if (deleteExisting) {
            eventRepository.deleteByOrganizationIdAndIdSubscriptionId(
                    organizationId,
                    subscription.getId()
            );
            eventRepository.flush();
        }

        List<WebhookSubscriptionEvent> events = eventTypes.stream()
                .map(eventType -> subscriptionEvent(
                        subscription,
                        organizationId,
                        eventType
                ))
                .toList();
        return eventRepository.saveAll(events);
    }

    private WebhookSubscriptionEvent subscriptionEvent(
            WebhookSubscription subscription,
            Long organizationId,
            WebhookEventType eventType
    ) {
        WebhookSubscriptionEvent event = new WebhookSubscriptionEvent();
        event.setId(new WebhookSubscriptionEventId(
                subscription.getId(),
                eventType
        ));
        event.setSubscription(subscription);
        event.setOrganizationId(organizationId);
        return event;
    }

    private Set<WebhookEventType> parseEventTypes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one webhook event type is required"
            );
        }
        return values.stream()
                .map(WebhookEventType::fromValue)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> eventTypeValues(Set<WebhookEventType> eventTypes) {
        return eventTypes.stream()
                .map(WebhookEventType::getValue)
                .toList();
    }

    private void ensureCapacity(Long organizationId) {
        long count = subscriptionRepository
                .countByOrganizationIdAndStatusNot(
                        organizationId,
                        WebhookSubscriptionStatus.REVOKED
                );
        if (count >= MAX_SUBSCRIPTIONS_PER_ORGANIZATION) {
            throw new IllegalArgumentException(
                    "Webhook subscription limit reached"
            );
        }
    }

    private void requireFeature(Long organizationId) {
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.WEBHOOKS
        );
    }

    private void requireNotRevoked(WebhookSubscription subscription) {
        if (subscription.getStatus() == WebhookSubscriptionStatus.REVOKED) {
            throw new IllegalArgumentException(
                    "A revoked webhook subscription cannot be changed"
            );
        }
    }

    private void applyCurrentSecret(
            WebhookSubscription subscription,
            GeneratedWebhookSecret generated
    ) {
        EncryptedWebhookSecret encrypted = generated.encryptedSecret();
        subscription.setSecretDisplaySuffix(generated.displaySuffix());
        subscription.setCurrentSecretCiphertext(encrypted.ciphertext());
        subscription.setCurrentSecretNonce(encrypted.nonce());
        subscription.setCurrentSecretTag(encrypted.authenticationTag());
        subscription.setCurrentSecretKeyVersion(encrypted.keyVersion());
    }

    private void moveCurrentSecretToPrevious(
            WebhookSubscription subscription,
            LocalDateTime now
    ) {
        subscription.setPreviousSecretCiphertext(
                cloneBytes(subscription.getCurrentSecretCiphertext())
        );
        subscription.setPreviousSecretNonce(
                cloneBytes(subscription.getCurrentSecretNonce())
        );
        subscription.setPreviousSecretTag(
                cloneBytes(subscription.getCurrentSecretTag())
        );
        subscription.setPreviousSecretKeyVersion(
                subscription.getCurrentSecretKeyVersion()
        );
        subscription.setPreviousSecretExpiresAt(
                now.plusHours(PREVIOUS_SECRET_GRACE_HOURS)
        );
    }

    private void clearPreviousSecret(WebhookSubscription subscription) {
        subscription.setPreviousSecretCiphertext(null);
        subscription.setPreviousSecretNonce(null);
        subscription.setPreviousSecretTag(null);
        subscription.setPreviousSecretKeyVersion(null);
        subscription.setPreviousSecretExpiresAt(null);
    }

    private byte[] cloneBytes(byte[] value) {
        return value == null ? null : value.clone();
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException(
                "Webhook subscription not found"
        );
    }
}
