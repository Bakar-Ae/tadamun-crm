package com.crm.backend.integration.delivery;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.integration.IntegrationAuditAction;
import com.crm.backend.integration.IntegrationAuditService;
import com.crm.backend.integration.IntegrationConnection;
import com.crm.backend.integration.IntegrationConnectionService;
import com.crm.backend.integration.IntegrationCredentialService;
import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.IntegrationPublicIdGenerator;
import com.crm.backend.integration.delivery.dto.IntegrationDeliveryAttemptResponse;
import com.crm.backend.integration.delivery.dto.IntegrationDeliveryResponse;
import com.crm.backend.integration.delivery.dto.QueueIntegrationDeliveryRequest;
import com.crm.backend.integration.provider.IntegrationCapability;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
import com.crm.backend.integration.provider.IntegrationOutboundMessage;
import com.crm.backend.integration.provider.IntegrationProviderAdapter;
import com.crm.backend.integration.provider.IntegrationProviderContext;
import com.crm.backend.integration.provider.IntegrationProviderDeliveryResult;
import com.crm.backend.integration.provider.IntegrationProviderException;
import com.crm.backend.integration.provider.IntegrationProviderRegistry;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class IntegrationDeliveryService {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile(
            "[A-Za-z0-9._:-]{1,100}"
    );
    private static final TypeReference<Map<String, Object>> CONFIG_MAP =
            new TypeReference<>() {
            };

    private final IntegrationDeliveryRepository deliveryRepository;
    private final IntegrationDeliveryAttemptRepository attemptRepository;
    private final IntegrationConnectionService connectionService;
    private final IntegrationCredentialService credentialService;
    private final IntegrationProviderRegistry providerRegistry;
    private final IntegrationPublicIdGenerator idGenerator;
    private final IntegrationDeliveryMapper mapper;
    private final IntegrationDeliveryRetryPolicy retryPolicy;
    private final IntegrationDeliveryWorkerProperties workerProperties;
    private final IntegrationAuditService auditService;
    private final CurrentOrganizationProvider organizationProvider;
    private final SubscriptionTimeProvider timeProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public IntegrationDeliveryService(
            IntegrationDeliveryRepository deliveryRepository,
            IntegrationDeliveryAttemptRepository attemptRepository,
            IntegrationConnectionService connectionService,
            IntegrationCredentialService credentialService,
            IntegrationProviderRegistry providerRegistry,
            IntegrationPublicIdGenerator idGenerator,
            IntegrationDeliveryMapper mapper,
            IntegrationDeliveryRetryPolicy retryPolicy,
            IntegrationDeliveryWorkerProperties workerProperties,
            IntegrationAuditService auditService,
            CurrentOrganizationProvider organizationProvider,
            SubscriptionTimeProvider timeProvider,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.connectionService = connectionService;
        this.credentialService = credentialService;
        this.providerRegistry = providerRegistry;
        this.idGenerator = idGenerator;
        this.mapper = mapper;
        this.retryPolicy = retryPolicy;
        this.workerProperties = workerProperties;
        this.auditService = auditService;
        this.organizationProvider = organizationProvider;
        this.timeProvider = timeProvider;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public Page<IntegrationDeliveryResponse> getDeliveries(
            Long connectionId,
            Pageable pageable
    ) {
        return deliveryRepository
                .findByOrganizationIdAndConnectionIdOrderByCreatedAtDesc(
                        organizationProvider.getOrganizationId(),
                        connectionId,
                        pageable
                )
                .map(mapper::toResponse);
    }

    public IntegrationDeliveryResponse getDelivery(String publicDeliveryId) {
        return mapper.toResponse(find(publicDeliveryId));
    }

    public List<IntegrationDeliveryAttemptResponse> getAttempts(
            String publicDeliveryId
    ) {
        IntegrationDelivery delivery = find(publicDeliveryId);
        return attemptRepository
                .findByOrganizationIdAndDeliveryIdOrderByAttemptNumberAsc(
                        organizationProvider.getOrganizationId(),
                        delivery.getId()
                ).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public IntegrationDeliveryResponse queue(
            Long connectionId,
            QueueIntegrationDeliveryRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        String idempotencyKey = normalizeIdempotencyKey(
                request.idempotencyKey()
        );
        var existing = deliveryRepository
                .findByOrganizationIdAndIdempotencyKey(
                        organizationId,
                        idempotencyKey
                );
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get());
        }

        IntegrationConnection connection = connectionService.requireActive(
                connectionId
        );
        validateMessage(connection, request);
        IntegrationDelivery delivery = new IntegrationDelivery();
        delivery.setPublicDeliveryId(idGenerator.deliveryId());
        delivery.setOrganization(
                organizationProvider.getOrganizationReference()
        );
        delivery.setConnection(connection);
        delivery.setProvider(connection.getProvider());
        delivery.setDeliveryType(request.type());
        delivery.setDestination(request.destination().trim());
        delivery.setSubject(normalizeNullable(request.subject()));
        delivery.setMessageBody(request.body());
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setStatus(IntegrationDeliveryStatus.PENDING);
        delivery.setMaximumAttempts(workerProperties.getMaximumAttempts());
        delivery.setNextAttemptAt(timeProvider.now());
        delivery.setCreatedByUser(userRepository.getReferenceById(actorUserId));
        IntegrationDelivery saved = deliveryRepository.saveAndFlush(delivery);
        auditService.log(
                organizationId,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_DELIVERY_QUEUED,
                connection.getId(),
                auditService.details(
                        "publicConnectionId",
                        connection.getPublicConnectionId(),
                        "publicDeliveryId", saved.getPublicDeliveryId(),
                        "provider", saved.getProvider().name(),
                        "deliveryType", saved.getDeliveryType().name()
                )
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationDeliveryStatus deliver(Long id, String claimToken) {
        IntegrationDelivery delivery = deliveryRepository
                .findForWorkerUpdate(id)
                .filter(value -> Objects.equals(
                        claimToken, value.getClaimToken()
                ))
                .filter(value -> value.getStatus()
                        == IntegrationDeliveryStatus.PROCESSING)
                .orElse(null);
        if (delivery == null) {
            return null;
        }

        long startedAt = System.nanoTime();
        try {
            IntegrationProviderAdapter adapter = providerRegistry.require(
                    delivery.getProvider()
            );
            IntegrationConnection connection = delivery.getConnection();
            IntegrationProviderContext context = new IntegrationProviderContext(
                    delivery.getOrganization().getId(),
                    connection.getPublicConnectionId(),
                    readConfiguration(connection),
                    credentialService.load(connection)
            );
            IntegrationProviderDeliveryResult result = adapter.deliver(
                    context,
                    new IntegrationOutboundMessage(
                            delivery.getDeliveryType(),
                            delivery.getDestination(),
                            delivery.getSubject(),
                            delivery.getMessageBody(),
                            delivery.getIdempotencyKey()
                    )
            );
            recordSuccess(delivery, result, elapsedMillis(startedAt));
            return delivery.getStatus();
        } catch (IntegrationProviderException exception) {
            recordFailure(
                    delivery,
                    exception.getCategory(),
                    exception.getMessage(),
                    exception.isRetryable(),
                    elapsedMillis(startedAt)
            );
            return delivery.getStatus();
        } catch (IllegalArgumentException exception) {
            recordFailure(
                    delivery,
                    "INVALID_MESSAGE",
                    exception.getMessage(),
                    false,
                    elapsedMillis(startedAt)
            );
            return delivery.getStatus();
        }
    }

    @Transactional
    public IntegrationDeliveryResponse retry(
            String publicDeliveryId,
            Long actorUserId
    ) {
        IntegrationDelivery delivery = find(publicDeliveryId);
        if (delivery.getStatus() != IntegrationDeliveryStatus.DEAD
                && delivery.getStatus()
                != IntegrationDeliveryStatus.TERMINAL_FAILURE) {
            throw new IllegalArgumentException(
                    "Only failed deliveries can be retried"
            );
        }
        if (delivery.getAttemptCount() >= 10) {
            throw new IllegalArgumentException(
                    "Maximum manual delivery attempts reached"
            );
        }
        delivery.setMaximumAttempts(Math.max(
                delivery.getMaximumAttempts(),
                delivery.getAttemptCount() + 1
        ));
        delivery.setStatus(IntegrationDeliveryStatus.RETRY_SCHEDULED);
        delivery.setNextAttemptAt(timeProvider.now());
        delivery.setCompletedAt(null);
        delivery.setLastErrorCategory(null);
        delivery.setLastErrorMessage(null);
        IntegrationDelivery saved = deliveryRepository.saveAndFlush(delivery);
        auditDeliveryLifecycle(
                saved,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_DELIVERY_RETRIED
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationDeliveryResponse cancel(
            String publicDeliveryId,
            Long actorUserId
    ) {
        IntegrationDelivery delivery = find(publicDeliveryId);
        if (delivery.getStatus() != IntegrationDeliveryStatus.PENDING
                && delivery.getStatus()
                != IntegrationDeliveryStatus.RETRY_SCHEDULED) {
            throw new IllegalArgumentException(
                    "Only pending deliveries can be cancelled"
            );
        }
        delivery.setStatus(IntegrationDeliveryStatus.CANCELLED);
        delivery.setCompletedAt(timeProvider.now());
        IntegrationDelivery saved = deliveryRepository.saveAndFlush(delivery);
        auditDeliveryLifecycle(
                saved,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_DELIVERY_CANCELLED
        );
        return mapper.toResponse(saved);
    }

    private IntegrationDelivery find(String publicDeliveryId) {
        return deliveryRepository.findByPublicDeliveryIdAndOrganizationId(
                publicDeliveryId,
                organizationProvider.getOrganizationId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Integration delivery not found"
        ));
    }

    private void validateMessage(
            IntegrationConnection connection,
            QueueIntegrationDeliveryRequest request
    ) {
        IntegrationProviderAdapter adapter = providerRegistry.require(
                connection.getProvider()
        );
        boolean supported = request.type() == IntegrationDeliveryType.EMAIL
                ? adapter.capabilities().contains(
                        IntegrationCapability.SEND_EMAIL
                )
                : adapter.capabilities().contains(
                        IntegrationCapability.SEND_MESSAGE
                );
        if (!supported) {
            throw new IllegalArgumentException(
                    "Delivery type is not supported by this integration"
            );
        }
        if (request.type() == IntegrationDeliveryType.EMAIL
                && normalizeNullable(request.subject()) == null) {
            throw new IllegalArgumentException("Email subject is required");
        }
        if (connection.getProvider() == IntegrationProvider.WHATSAPP_CLOUD
                && request.type() != IntegrationDeliveryType.WHATSAPP_TEXT) {
            throw new IllegalArgumentException(
                    "WhatsApp connections require WHATSAPP_TEXT"
            );
        }
        if (connection.getProvider() == IntegrationProvider.SMTP
                && request.type() != IntegrationDeliveryType.EMAIL) {
            throw new IllegalArgumentException(
                    "SMTP connections require EMAIL"
            );
        }
    }

    private Map<String, Object> readConfiguration(
            IntegrationConnection connection
    ) {
        try {
            return objectMapper.readValue(
                    connection.getConfiguration(), CONFIG_MAP
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Stored integration configuration is invalid",
                    exception
            );
        }
    }

    private String normalizeIdempotencyKey(String value) {
        String key = value == null || value.isBlank()
                ? idGenerator.idempotencyKey()
                : value.trim();
        if (!IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency key contains unsupported characters"
            );
        }
        return key;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void recordSuccess(
            IntegrationDelivery delivery,
            IntegrationProviderDeliveryResult result,
            int durationMs
    ) {
        delivery.setStatus(IntegrationDeliveryStatus.SUCCEEDED);
        delivery.setProviderMessageId(result.providerMessageId());
        delivery.setCompletedAt(timeProvider.now());
        delivery.setLastErrorCategory(null);
        delivery.setLastErrorMessage(null);
        clearClaim(delivery);
        saveAttempt(
                delivery,
                IntegrationDeliveryOutcome.SUCCEEDED,
                durationMs,
                result.providerMessageId(),
                null,
                null
        );
        auditDeliveryLifecycle(
                delivery,
                delivery.getCreatedByUser() == null
                        ? null : delivery.getCreatedByUser().getId(),
                IntegrationAuditAction.INTEGRATION_DELIVERY_SUCCEEDED
        );
    }

    private void recordFailure(
            IntegrationDelivery delivery,
            String category,
            String message,
            boolean retryable,
            int durationMs
    ) {
        LocalDateTime now = timeProvider.now();
        boolean exhausted = delivery.getAttemptCount()
                >= delivery.getMaximumAttempts();
        IntegrationDeliveryOutcome outcome;
        if (retryable && !exhausted) {
            delivery.setStatus(IntegrationDeliveryStatus.RETRY_SCHEDULED);
            delivery.setNextAttemptAt(retryPolicy.nextAttemptAt(
                    delivery.getAttemptCount(), now
            ));
            outcome = IntegrationDeliveryOutcome.RETRYABLE_FAILURE;
        } else {
            delivery.setStatus(retryable
                    ? IntegrationDeliveryStatus.DEAD
                    : IntegrationDeliveryStatus.TERMINAL_FAILURE);
            delivery.setCompletedAt(now);
            outcome = IntegrationDeliveryOutcome.TERMINAL_FAILURE;
        }
        delivery.setLastErrorCategory(safe(category, 60));
        delivery.setLastErrorMessage(safe(message, MAX_ERROR_LENGTH));
        clearClaim(delivery);
        saveAttempt(
                delivery,
                outcome,
                durationMs,
                null,
                delivery.getLastErrorCategory(),
                delivery.getLastErrorMessage()
        );
        auditDeliveryLifecycle(
                delivery,
                delivery.getCreatedByUser() == null
                        ? null : delivery.getCreatedByUser().getId(),
                IntegrationAuditAction.INTEGRATION_DELIVERY_FAILED
        );
    }

    private void saveAttempt(
            IntegrationDelivery delivery,
            IntegrationDeliveryOutcome outcome,
            int durationMs,
            String providerMessageId,
            String errorCategory,
            String errorMessage
    ) {
        IntegrationDeliveryAttempt attempt = new IntegrationDeliveryAttempt();
        attempt.setOrganization(delivery.getOrganization());
        attempt.setDelivery(delivery);
        attempt.setAttemptNumber(delivery.getAttemptCount());
        attempt.setOutcome(outcome);
        attempt.setDurationMs(durationMs);
        attempt.setProviderMessageId(providerMessageId);
        attempt.setErrorCategory(errorCategory);
        attempt.setErrorMessage(errorMessage);
        attemptRepository.save(attempt);
    }

    private void clearClaim(IntegrationDelivery delivery) {
        delivery.setClaimedAt(null);
        delivery.setClaimToken(null);
    }

    private int elapsedMillis(long startedAt) {
        long millis = (System.nanoTime() - startedAt) / 1_000_000;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, millis));
    }

    private String safe(String value, int maxLength) {
        String result = value == null || value.isBlank()
                ? "UNKNOWN" : value.trim();
        return result.length() <= maxLength
                ? result : result.substring(0, maxLength);
    }

    private void auditDeliveryLifecycle(
            IntegrationDelivery delivery,
            Long actorUserId,
            IntegrationAuditAction action
    ) {
        auditService.log(
                delivery.getOrganization().getId(),
                actorUserId,
                action,
                delivery.getConnection().getId(),
                auditService.details(
                        "publicConnectionId",
                        delivery.getConnection().getPublicConnectionId(),
                        "publicDeliveryId", delivery.getPublicDeliveryId(),
                        "provider", delivery.getProvider().name(),
                        "deliveryType", delivery.getDeliveryType().name(),
                        "status", delivery.getStatus().name(),
                        "attemptCount", delivery.getAttemptCount(),
                        "errorCategory", delivery.getLastErrorCategory()
                )
        );
    }
}
