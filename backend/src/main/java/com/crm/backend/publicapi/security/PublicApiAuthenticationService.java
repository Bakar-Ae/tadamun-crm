package com.crm.backend.publicapi.security;

import com.crm.backend.publicapi.PublicApiAuditAction;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.PublicApiKey;
import com.crm.backend.publicapi.key.PublicApiKeyRepository;
import com.crm.backend.publicapi.key.PublicApiKeyStatus;
import com.crm.backend.publicapi.key.PublicApiKeyTokenService;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PublicApiAuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            PublicApiAuthenticationService.class
    );

    private final PublicApiKeyRepository apiKeyRepository;
    private final PublicApiKeyTokenService tokenService;
    private final SubscriptionFeatureAccessService featureAccessService;
    private final SubscriptionTimeProvider timeProvider;
    private final PublicApiAuditService auditService;

    public PublicApiAuthenticationService(
            PublicApiKeyRepository apiKeyRepository,
            PublicApiKeyTokenService tokenService,
            SubscriptionFeatureAccessService featureAccessService,
            SubscriptionTimeProvider timeProvider,
            PublicApiAuditService auditService
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.tokenService = tokenService;
        this.featureAccessService = featureAccessService;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    @Transactional
    public PublicApiPrincipal authenticate(String rawKey) {
        String publicId = tokenService.extractPublicId(rawKey)
                .orElseGet(() -> {
                    auditUnknownFailure();
                    throw new PublicApiAuthenticationException();
                });
        PublicApiKey apiKey = apiKeyRepository.findByPublicId(publicId)
                .orElseGet(() -> {
                    auditUnknownFailure();
                    throw new PublicApiAuthenticationException();
                });
        LocalDateTime now = timeProvider.now();
        boolean validSecret = tokenService.matches(
                rawKey,
                apiKey.getPublicId(),
                apiKey.getSecretHash()
        );
        boolean expired = apiKey.getExpiresAt() != null
                && !apiKey.getExpiresAt().isAfter(now);

        if (!validSecret
                || apiKey.getStatus() != PublicApiKeyStatus.ACTIVE
                || expired) {
            auditKnownFailure(apiKey);
            throw new PublicApiAuthenticationException();
        }

        Long organizationId = apiKey.getOrganization().getId();
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.PUBLIC_API
        );

        apiKey.setLastUsedAt(now);
        return new PublicApiPrincipal(
                apiKey.getId(),
                organizationId,
                apiKey.getName(),
                apiKey.getScopes(),
                apiKey.getRateLimitPerMinute()
        );
    }

    private void auditUnknownFailure() {
        try {
            auditService.logPlatformAuthenticationFailure();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist public API authentication audit");
        }
    }

    private void auditKnownFailure(PublicApiKey apiKey) {
        try {
            auditService.logSecurityEventForOrganization(
                    apiKey.getOrganization().getId(),
                    PublicApiAuditAction.PUBLIC_API_AUTHENTICATION_FAILED,
                    apiKey.getId(),
                    auditService.details("reason", "invalid_credentials")
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist public API authentication audit");
        }
    }
}
