package com.crm.backend.publicapi.key;

import com.crm.backend.organization.Organization;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.dto.CreatePublicApiKeyRequest;
import com.crm.backend.publicapi.key.dto.CreatedPublicApiKeyResponse;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicApiKeyServiceTest {

    private PublicApiKeyRepository apiKeyRepository;
    private PublicApiKeyTokenService tokenService;
    private CurrentOrganizationProvider organizationProvider;
    private UserRepository userRepository;
    private SubscriptionFeatureAccessService featureAccessService;
    private SubscriptionTimeProvider timeProvider;
    private PublicApiAuditService auditService;
    private PublicApiKeyService apiKeyService;
    private Organization organization;
    private User actor;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(PublicApiKeyRepository.class);
        tokenService = mock(PublicApiKeyTokenService.class);
        organizationProvider = mock(CurrentOrganizationProvider.class);
        userRepository = mock(UserRepository.class);
        featureAccessService = mock(SubscriptionFeatureAccessService.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditService = mock(PublicApiAuditService.class);
        apiKeyService = new PublicApiKeyService(
                apiKeyRepository,
                tokenService,
                new PublicApiKeyMapper(),
                organizationProvider,
                userRepository,
                featureAccessService,
                timeProvider,
                auditService
        );

        organization = new Organization();
        organization.setId(42L);
        actor = new User();
        actor.setId(7L);
        actor.setFullName("API Administrator");
        when(organizationProvider.getOrganizationId()).thenReturn(42L);
        when(organizationProvider.getOrganizationReference())
                .thenReturn(organization);
        when(userRepository.getReferenceById(7L)).thenReturn(actor);
    }

    @Test
    void createShouldReturnRawKeyOnceAndPersistOnlyItsHash() {
        GeneratedPublicApiKey generated = new GeneratedPublicApiKey(
                "tdm_live_publicidentifier.secret-value",
                "publicidentifier",
                "tdm_live_publicidentifier.secret...",
                "hashed-secret"
        );
        when(tokenService.generate()).thenReturn(generated);
        when(apiKeyRepository.existsByPublicId("publicidentifier"))
                .thenReturn(false);
        when(apiKeyRepository.saveAndFlush(any(PublicApiKey.class)))
                .thenAnswer(invocation -> {
                    PublicApiKey key = invocation.getArgument(0);
                    key.setId(11L);
                    return key;
                });

        CreatedPublicApiKeyResponse response = apiKeyService.createKey(
                new CreatePublicApiKeyRequest(
                        " Reporting integration ",
                        Set.of("customers:read"),
                        null,
                        null
                ),
                7L
        );

        ArgumentCaptor<PublicApiKey> keyCaptor =
                ArgumentCaptor.forClass(PublicApiKey.class);
        verify(apiKeyRepository).saveAndFlush(keyCaptor.capture());
        PublicApiKey stored = keyCaptor.getValue();

        assertEquals(generated.rawKey(), response.apiKey());
        assertEquals(generated.displayPrefix(), response.key().displayPrefix());
        assertEquals("hashed-secret", stored.getSecretHash());
        assertNotEquals(generated.rawKey(), stored.getSecretHash());
        assertEquals(60, stored.getRateLimitPerMinute());
        assertEquals(Set.of(PublicApiScope.CUSTOMERS_READ), stored.getScopes());
        verify(featureAccessService).requireFeature(
                42L,
                SubscriptionFeature.PUBLIC_API
        );
    }

    @Test
    void rotateShouldCreateReplacementBeforeRevokingCurrentKey() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 12, 0);
        PublicApiKey current = currentKey();
        GeneratedPublicApiKey generated = new GeneratedPublicApiKey(
                "tdm_live_newpublicident.new-secret",
                "newpublicident",
                "tdm_live_newpublicident.newsec...",
                "new-hash"
        );
        when(apiKeyRepository.findForUpdate(11L, 42L))
                .thenReturn(java.util.Optional.of(current));
        when(tokenService.generate()).thenReturn(generated);
        when(apiKeyRepository.existsByPublicId("newpublicident"))
                .thenReturn(false);
        when(timeProvider.now()).thenReturn(now);
        when(apiKeyRepository.saveAndFlush(any(PublicApiKey.class)))
                .thenAnswer(invocation -> {
                    PublicApiKey key = invocation.getArgument(0);
                    if (key.getId() == null) {
                        key.setId(12L);
                    }
                    return key;
                });

        CreatedPublicApiKeyResponse response =
                apiKeyService.rotateKey(11L, 7L);

        assertEquals(generated.rawKey(), response.apiKey());
        assertEquals(PublicApiKeyStatus.REVOKED, current.getStatus());
        assertEquals(now, current.getRevokedAt());
        assertSame(actor, current.getRevokedByUser());

        ArgumentCaptor<PublicApiKey> keyCaptor =
                ArgumentCaptor.forClass(PublicApiKey.class);
        verify(apiKeyRepository, org.mockito.Mockito.times(2))
                .saveAndFlush(keyCaptor.capture());
        PublicApiKey replacement = keyCaptor.getAllValues().get(0);
        assertSame(current, replacement.getRotatedFromKey());
        assertEquals(PublicApiKeyStatus.ACTIVE, replacement.getStatus());
        assertEquals("new-hash", replacement.getSecretHash());
    }

    private PublicApiKey currentKey() {
        PublicApiKey key = new PublicApiKey();
        key.setId(11L);
        key.setOrganization(organization);
        key.setName("Reporting integration");
        key.setPublicId("oldpublicidentif");
        key.setDisplayPrefix("tdm_live_oldpublicidentif.oldsec...");
        key.setSecretHash("old-hash");
        key.setStatus(PublicApiKeyStatus.ACTIVE);
        key.setRateLimitPerMinute(60);
        key.setScopes(new LinkedHashSet<>(Set.of(
                PublicApiScope.CUSTOMERS_READ
        )));
        key.setCreatedByUser(actor);
        return key;
    }
}
