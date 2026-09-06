package com.crm.backend.publicapi.security;

import com.crm.backend.organization.Organization;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.PublicApiKey;
import com.crm.backend.publicapi.key.PublicApiKeyRepository;
import com.crm.backend.publicapi.key.PublicApiKeyStatus;
import com.crm.backend.publicapi.key.PublicApiKeyTokenService;
import com.crm.backend.publicapi.key.PublicApiScope;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicApiAuthenticationServiceTest {

    private static final String RAW_KEY =
            "tdm_live_abcdefghijklmnop.abcdefghijklmnopqrstuvwxyzABCDEFGHijklmno";

    private PublicApiKeyRepository apiKeyRepository;
    private PublicApiKeyTokenService tokenService;
    private SubscriptionFeatureAccessService featureAccessService;
    private SubscriptionTimeProvider timeProvider;
    private PublicApiAuditService auditService;
    private PublicApiAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(PublicApiKeyRepository.class);
        tokenService = mock(PublicApiKeyTokenService.class);
        featureAccessService = mock(SubscriptionFeatureAccessService.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditService = mock(PublicApiAuditService.class);
        authenticationService = new PublicApiAuthenticationService(
                apiKeyRepository,
                tokenService,
                featureAccessService,
                timeProvider,
                auditService
        );
    }

    @Test
    void activeKeyShouldAuthenticateForItsOrganization() {
        PublicApiKey apiKey = apiKey(PublicApiKeyStatus.ACTIVE, null);
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 12, 0);
        when(tokenService.extractPublicId(RAW_KEY))
                .thenReturn(Optional.of("abcdefghijklmnop"));
        when(apiKeyRepository.findByPublicId("abcdefghijklmnop"))
                .thenReturn(Optional.of(apiKey));
        when(tokenService.matches(RAW_KEY, "abcdefghijklmnop", "stored-hash"))
                .thenReturn(true);
        when(timeProvider.now()).thenReturn(now);

        PublicApiPrincipal principal =
                authenticationService.authenticate(RAW_KEY);

        assertEquals(9L, principal.apiKeyId());
        assertEquals(42L, principal.organizationId());
        assertEquals(Set.of(PublicApiScope.CUSTOMERS_READ), principal.scopes());
        assertEquals(now, apiKey.getLastUsedAt());
        verify(featureAccessService).requireFeature(
                42L,
                SubscriptionFeature.PUBLIC_API
        );
    }

    @Test
    void revokedKeyShouldBeRejectedBeforeSubscriptionAccess() {
        PublicApiKey apiKey = apiKey(PublicApiKeyStatus.REVOKED, null);
        when(tokenService.extractPublicId(RAW_KEY))
                .thenReturn(Optional.of("abcdefghijklmnop"));
        when(apiKeyRepository.findByPublicId("abcdefghijklmnop"))
                .thenReturn(Optional.of(apiKey));
        when(tokenService.matches(RAW_KEY, "abcdefghijklmnop", "stored-hash"))
                .thenReturn(true);
        when(timeProvider.now()).thenReturn(
                LocalDateTime.of(2026, 9, 6, 12, 0)
        );

        assertThrows(
                PublicApiAuthenticationException.class,
                () -> authenticationService.authenticate(RAW_KEY)
        );
        verify(featureAccessService, never()).requireFeature(
                42L,
                SubscriptionFeature.PUBLIC_API
        );
    }

    @Test
    void expiredKeyShouldBeRejected() {
        PublicApiKey apiKey = apiKey(
                PublicApiKeyStatus.ACTIVE,
                LocalDateTime.of(2026, 9, 6, 11, 59)
        );
        when(tokenService.extractPublicId(RAW_KEY))
                .thenReturn(Optional.of("abcdefghijklmnop"));
        when(apiKeyRepository.findByPublicId("abcdefghijklmnop"))
                .thenReturn(Optional.of(apiKey));
        when(tokenService.matches(RAW_KEY, "abcdefghijklmnop", "stored-hash"))
                .thenReturn(true);
        when(timeProvider.now()).thenReturn(
                LocalDateTime.of(2026, 9, 6, 12, 0)
        );

        assertThrows(
                PublicApiAuthenticationException.class,
                () -> authenticationService.authenticate(RAW_KEY)
        );
    }

    @Test
    void invalidFormatShouldNotQueryKeyRepository() {
        when(tokenService.extractPublicId("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(
                PublicApiAuthenticationException.class,
                () -> authenticationService.authenticate("invalid")
        );
        verify(apiKeyRepository, never()).findByPublicId("invalid");
        verify(auditService).logPlatformAuthenticationFailure();
    }

    private PublicApiKey apiKey(
            PublicApiKeyStatus status,
            LocalDateTime expiresAt
    ) {
        Organization organization = new Organization();
        organization.setId(42L);
        PublicApiKey apiKey = new PublicApiKey();
        apiKey.setId(9L);
        apiKey.setOrganization(organization);
        apiKey.setName("Reporting integration");
        apiKey.setPublicId("abcdefghijklmnop");
        apiKey.setSecretHash("stored-hash");
        apiKey.setStatus(status);
        apiKey.setRateLimitPerMinute(60);
        apiKey.setScopes(new LinkedHashSet<>(Set.of(
                PublicApiScope.CUSTOMERS_READ
        )));
        apiKey.setExpiresAt(expiresAt);
        return apiKey;
    }
}
