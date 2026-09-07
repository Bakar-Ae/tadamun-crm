package com.crm.backend.webhook;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.organization.Organization;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookSubscriptionServiceTest {

    private WebhookSubscriptionRepository subscriptionRepository;
    private WebhookSubscriptionEventRepository eventRepository;
    private WebhookSecretEncryptionService secretEncryptionService;
    private WebhookEndpointValidator endpointValidator;
    private CurrentOrganizationProvider organizationProvider;
    private UserRepository userRepository;
    private SubscriptionFeatureAccessService featureAccessService;
    private SubscriptionTimeProvider timeProvider;
    private WebhookAuditService auditService;
    private WebhookSubscriptionService service;
    private Organization organization;
    private User actor;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(WebhookSubscriptionRepository.class);
        eventRepository = mock(WebhookSubscriptionEventRepository.class);
        secretEncryptionService = mock(
                WebhookSecretEncryptionService.class
        );
        endpointValidator = mock(WebhookEndpointValidator.class);
        organizationProvider = mock(CurrentOrganizationProvider.class);
        userRepository = mock(UserRepository.class);
        featureAccessService = mock(SubscriptionFeatureAccessService.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditService = mock(WebhookAuditService.class);
        service = new WebhookSubscriptionService(
                subscriptionRepository,
                eventRepository,
                secretEncryptionService,
                endpointValidator,
                new WebhookSubscriptionMapper(),
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
        actor.setFullName("Webhook Administrator");
        when(organizationProvider.getOrganizationId()).thenReturn(42L);
        when(organizationProvider.getOrganizationReference())
                .thenReturn(organization);
        when(userRepository.getReferenceById(7L)).thenReturn(actor);
        when(endpointValidator.validateAndNormalize(any()))
                .thenReturn("https://hooks.example.com/events");
        when(eventRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.details(any())).thenReturn(java.util.Map.of());
    }

    @Test
    void createShouldReturnRawSecretOnceAndPersistEncryptedSecret() {
        GeneratedWebhookSecret generated = generatedSecret(
                "whsec_raw-secret",
                "w-secret",
                (byte) 11
        );
        when(secretEncryptionService.generate()).thenReturn(generated);
        when(subscriptionRepository.saveAndFlush(
                any(WebhookSubscription.class)
        )).thenAnswer(invocation -> {
            WebhookSubscription subscription = invocation.getArgument(0);
            subscription.setId(99L);
            return subscription;
        });

        WebhookSubscriptionSecretResponse response =
                service.createSubscription(
                        new CreateWebhookSubscriptionRequest(
                                " CRM events ",
                                " https://hooks.example.com/events ",
                                Set.of("customer.created", "lead.updated")
                        ),
                        7L
                );

        ArgumentCaptor<WebhookSubscription> captor =
                ArgumentCaptor.forClass(WebhookSubscription.class);
        verify(subscriptionRepository).saveAndFlush(captor.capture());
        WebhookSubscription stored = captor.getValue();

        assertEquals("whsec_raw-secret", response.signingSecret());
        assertEquals("CRM events", stored.getName());
        assertEquals(
                "https://hooks.example.com/events",
                stored.getEndpointUrl()
        );
        assertArrayEquals(
                generated.encryptedSecret().ciphertext(),
                stored.getCurrentSecretCiphertext()
        );
        assertEquals(
                Set.of("customer.created", "lead.updated"),
                response.subscription().eventTypes()
        );
        verify(featureAccessService).requireFeature(
                42L,
                SubscriptionFeature.WEBHOOKS
        );
    }

    @Test
    void getShouldUseOrganizationScopedRepositoryLookup() {
        when(subscriptionRepository.findByIdAndOrganizationId(9L, 42L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getSubscription(9L)
        );
        verify(subscriptionRepository).findByIdAndOrganizationId(9L, 42L);
    }

    @Test
    void createShouldStopAtOrganizationLimitBeforeGeneratingSecret() {
        when(subscriptionRepository.countByOrganizationIdAndStatusNot(
                42L,
                WebhookSubscriptionStatus.REVOKED
        )).thenReturn(20L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSubscription(
                        new CreateWebhookSubscriptionRequest(
                                "Overflow",
                                "https://hooks.example.com/events",
                                Set.of("customer.created")
                        ),
                        7L
                )
        );
        verify(secretEncryptionService, never()).generate();
    }

    @Test
    void updateShouldReplaceEventsWithoutChangingStatus() {
        WebhookSubscription subscription = activeSubscription();
        subscription.setStatus(WebhookSubscriptionStatus.DISABLED);
        when(subscriptionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.saveAndFlush(subscription))
                .thenReturn(subscription);

        WebhookSubscriptionResponse response = service.updateSubscription(
                99L,
                new UpdateWebhookSubscriptionRequest(
                        "Updated hook",
                        "https://hooks.example.com/updated",
                        Set.of("task.completed")
                ),
                7L
        );

        verify(eventRepository)
                .deleteByOrganizationIdAndIdSubscriptionId(42L, 99L);
        assertEquals(WebhookSubscriptionStatus.DISABLED, response.status());
        assertEquals(Set.of("task.completed"), response.eventTypes());
    }

    @Test
    void rotateShouldKeepPreviousEncryptedSecretForTwentyFourHours() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 10, 0);
        WebhookSubscription subscription = activeSubscription();
        byte[] oldCiphertext = subscription.getCurrentSecretCiphertext()
                .clone();
        GeneratedWebhookSecret replacement = generatedSecret(
                "whsec_replacement",
                "acement",
                (byte) 22
        );
        when(subscriptionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(subscription));
        when(secretEncryptionService.generate()).thenReturn(replacement);
        when(timeProvider.now()).thenReturn(now);
        when(subscriptionRepository.saveAndFlush(subscription))
                .thenReturn(subscription);
        when(eventRepository.findByOrganizationIdAndIdSubscriptionId(
                42L,
                99L
        )).thenReturn(List.of(subscriptionEvent(
                subscription,
                WebhookEventType.CUSTOMER_CREATED
        )));

        WebhookSubscriptionSecretResponse response = service.rotateSecret(
                99L,
                7L
        );

        assertEquals("whsec_replacement", response.signingSecret());
        assertArrayEquals(
                oldCiphertext,
                subscription.getPreviousSecretCiphertext()
        );
        assertEquals(
                now.plusHours(24),
                subscription.getPreviousSecretExpiresAt()
        );
        assertArrayEquals(
                replacement.encryptedSecret().ciphertext(),
                subscription.getCurrentSecretCiphertext()
        );
    }

    @Test
    void revokeShouldClearAllDecryptableSecretsAndBeIdempotent() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 11, 0);
        WebhookSubscription subscription = activeSubscription();
        subscription.setPreviousSecretCiphertext(new byte[]{4});
        subscription.setPreviousSecretNonce(new byte[12]);
        subscription.setPreviousSecretTag(new byte[16]);
        subscription.setPreviousSecretKeyVersion("v0");
        subscription.setPreviousSecretExpiresAt(now.plusHours(1));
        when(subscriptionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(subscription));
        when(timeProvider.now()).thenReturn(now);
        when(subscriptionRepository.saveAndFlush(subscription))
                .thenReturn(subscription);
        when(eventRepository.findByOrganizationIdAndIdSubscriptionId(
                42L,
                99L
        )).thenReturn(List.of());

        WebhookSubscriptionResponse response =
                service.revokeSubscription(99L, 7L);

        assertEquals(WebhookSubscriptionStatus.REVOKED, response.status());
        assertNull(subscription.getCurrentSecretCiphertext());
        assertNull(subscription.getCurrentSecretNonce());
        assertNull(subscription.getCurrentSecretTag());
        assertNull(subscription.getCurrentSecretKeyVersion());
        assertNull(subscription.getPreviousSecretCiphertext());
        assertNull(subscription.getPreviousSecretExpiresAt());
        assertEquals(now, subscription.getRevokedAt());

        WebhookSubscriptionResponse repeated =
                service.revokeSubscription(99L, 7L);
        assertEquals(WebhookSubscriptionStatus.REVOKED, repeated.status());
        verify(subscriptionRepository).saveAndFlush(subscription);
    }

    private WebhookSubscription activeSubscription() {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(99L);
        subscription.setOrganization(organization);
        subscription.setName("CRM events");
        subscription.setEndpointUrl("https://hooks.example.com/events");
        subscription.setStatus(WebhookSubscriptionStatus.ACTIVE);
        subscription.setSecretDisplaySuffix("original");
        subscription.setCurrentSecretCiphertext(new byte[]{1, 2, 3});
        subscription.setCurrentSecretNonce(new byte[12]);
        subscription.setCurrentSecretTag(new byte[16]);
        subscription.setCurrentSecretKeyVersion("v1");
        subscription.setCreatedByUser(actor);
        return subscription;
    }

    private GeneratedWebhookSecret generatedSecret(
            String rawSecret,
            String suffix,
            byte ciphertextValue
    ) {
        return new GeneratedWebhookSecret(
                rawSecret,
                suffix,
                new EncryptedWebhookSecret(
                        new byte[]{ciphertextValue},
                        new byte[12],
                        new byte[16],
                        "v1"
                )
        );
    }

    private WebhookSubscriptionEvent subscriptionEvent(
            WebhookSubscription subscription,
            WebhookEventType eventType
    ) {
        WebhookSubscriptionEvent event = new WebhookSubscriptionEvent();
        event.setId(new WebhookSubscriptionEventId(
                subscription.getId(),
                eventType
        ));
        event.setSubscription(subscription);
        event.setOrganizationId(42L);
        return event;
    }
}
