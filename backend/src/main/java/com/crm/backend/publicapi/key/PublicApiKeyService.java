package com.crm.backend.publicapi.key;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.publicapi.PublicApiAuditAction;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.dto.CreatePublicApiKeyRequest;
import com.crm.backend.publicapi.key.dto.CreatedPublicApiKeyResponse;
import com.crm.backend.publicapi.key.dto.PublicApiKeyResponse;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PublicApiKeyService {

    private static final int DEFAULT_RATE_LIMIT = 60;
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final PublicApiKeyRepository apiKeyRepository;
    private final PublicApiKeyTokenService tokenService;
    private final PublicApiKeyMapper apiKeyMapper;
    private final CurrentOrganizationProvider organizationProvider;
    private final UserRepository userRepository;
    private final SubscriptionFeatureAccessService featureAccessService;
    private final SubscriptionTimeProvider timeProvider;
    private final PublicApiAuditService auditService;

    public PublicApiKeyService(
            PublicApiKeyRepository apiKeyRepository,
            PublicApiKeyTokenService tokenService,
            PublicApiKeyMapper apiKeyMapper,
            CurrentOrganizationProvider organizationProvider,
            UserRepository userRepository,
            SubscriptionFeatureAccessService featureAccessService,
            SubscriptionTimeProvider timeProvider,
            PublicApiAuditService auditService
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.tokenService = tokenService;
        this.apiKeyMapper = apiKeyMapper;
        this.organizationProvider = organizationProvider;
        this.userRepository = userRepository;
        this.featureAccessService = featureAccessService;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    public Page<PublicApiKeyResponse> getKeys(Pageable pageable) {
        return apiKeyRepository.findByOrganizationId(
                organizationProvider.getOrganizationId(),
                pageable
        ).map(apiKeyMapper::toResponse);
    }

    @Transactional
    public CreatedPublicApiKeyResponse createKey(
            CreatePublicApiKeyRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.PUBLIC_API
        );

        GeneratedPublicApiKey generated = generateUniqueKey();
        PublicApiKey apiKey = new PublicApiKey();
        apiKey.setOrganization(organizationProvider.getOrganizationReference());
        apiKey.setName(request.name().trim());
        apiKey.setPublicId(generated.publicId());
        apiKey.setDisplayPrefix(generated.displayPrefix());
        apiKey.setSecretHash(generated.secretHash());
        apiKey.setStatus(PublicApiKeyStatus.ACTIVE);
        apiKey.setRateLimitPerMinute(resolveRateLimit(
                request.rateLimitPerMinute()
        ));
        apiKey.setScopes(parseScopes(request.scopes()));
        apiKey.setExpiresAt(request.expiresAt());
        apiKey.setCreatedByUser(userRepository.getReferenceById(actorUserId));

        PublicApiKey saved = apiKeyRepository.saveAndFlush(apiKey);
        auditService.logForOrganization(
                organizationId,
                actorUserId,
                PublicApiAuditAction.PUBLIC_API_KEY_CREATED,
                saved.getId(),
                auditService.details(
                        "name", saved.getName(),
                        "scopes", request.scopes(),
                        "rateLimitPerMinute", saved.getRateLimitPerMinute()
                )
        );

        return new CreatedPublicApiKeyResponse(
                generated.rawKey(),
                apiKeyMapper.toResponse(saved)
        );
    }

    @Transactional
    public CreatedPublicApiKeyResponse rotateKey(
            Long id,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.PUBLIC_API
        );
        PublicApiKey current = findForUpdate(id, organizationId);

        if (current.getStatus() != PublicApiKeyStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only an active public API key can be rotated"
            );
        }

        GeneratedPublicApiKey generated = generateUniqueKey();
        PublicApiKey replacement = new PublicApiKey();
        replacement.setOrganization(current.getOrganization());
        replacement.setName(current.getName());
        replacement.setPublicId(generated.publicId());
        replacement.setDisplayPrefix(generated.displayPrefix());
        replacement.setSecretHash(generated.secretHash());
        replacement.setStatus(PublicApiKeyStatus.ACTIVE);
        replacement.setRateLimitPerMinute(current.getRateLimitPerMinute());
        replacement.setScopes(new LinkedHashSet<>(current.getScopes()));
        replacement.setExpiresAt(activeExpiration(current.getExpiresAt()));
        replacement.setCreatedByUser(
                userRepository.getReferenceById(actorUserId)
        );
        replacement.setRotatedFromKey(current);

        PublicApiKey savedReplacement =
                apiKeyRepository.saveAndFlush(replacement);
        revoke(current, actorUserId, timeProvider.now());
        apiKeyRepository.saveAndFlush(current);

        auditService.logForOrganization(
                organizationId,
                actorUserId,
                PublicApiAuditAction.PUBLIC_API_KEY_ROTATED,
                savedReplacement.getId(),
                auditService.details(
                        "rotatedFromKeyId", current.getId(),
                        "name", savedReplacement.getName()
                )
        );

        return new CreatedPublicApiKeyResponse(
                generated.rawKey(),
                apiKeyMapper.toResponse(savedReplacement)
        );
    }

    @Transactional
    public PublicApiKeyResponse revokeKey(Long id, Long actorUserId) {
        Long organizationId = organizationProvider.getOrganizationId();
        PublicApiKey apiKey = findForUpdate(id, organizationId);

        if (apiKey.getStatus() == PublicApiKeyStatus.REVOKED) {
            return apiKeyMapper.toResponse(apiKey);
        }

        revoke(apiKey, actorUserId, timeProvider.now());
        PublicApiKey saved = apiKeyRepository.saveAndFlush(apiKey);
        auditService.logForOrganization(
                organizationId,
                actorUserId,
                PublicApiAuditAction.PUBLIC_API_KEY_REVOKED,
                saved.getId(),
                auditService.details("name", saved.getName())
        );
        return apiKeyMapper.toResponse(saved);
    }

    private PublicApiKey findForUpdate(Long id, Long organizationId) {
        return apiKeyRepository.findForUpdate(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Public API key not found"
                ));
    }

    private GeneratedPublicApiKey generateUniqueKey() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            GeneratedPublicApiKey generated = tokenService.generate();
            if (!apiKeyRepository.existsByPublicId(generated.publicId())) {
                return generated;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique public API key"
        );
    }

    private Set<PublicApiScope> parseScopes(Set<String> values) {
        LinkedHashSet<PublicApiScope> scopes = new LinkedHashSet<>();
        values.stream()
                .map(PublicApiScope::fromValue)
                .sorted()
                .forEach(scopes::add);
        return scopes;
    }

    private int resolveRateLimit(Integer requestedLimit) {
        return requestedLimit == null
                ? DEFAULT_RATE_LIMIT
                : requestedLimit;
    }

    private LocalDateTime activeExpiration(LocalDateTime expiration) {
        return expiration != null && expiration.isAfter(timeProvider.now())
                ? expiration
                : null;
    }

    private void revoke(
            PublicApiKey apiKey,
            Long actorUserId,
            LocalDateTime revokedAt
    ) {
        apiKey.setStatus(PublicApiKeyStatus.REVOKED);
        apiKey.setRevokedByUser(userRepository.getReferenceById(actorUserId));
        apiKey.setRevokedAt(revokedAt);
    }
}
