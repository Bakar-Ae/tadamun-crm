package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class WebhookPersistenceRepositoryTest {

    @Autowired
    private WebhookSubscriptionRepository subscriptionRepository;

    @Autowired
    private WebhookSubscriptionEventRepository subscriptionEventRepository;

    @Autowired
    private WebhookEventRepository eventRepository;

    @Autowired
    private WebhookDeliveryRepository deliveryRepository;

    @Autowired
    private WebhookDeliveryAttemptRepository attemptRepository;

    @Autowired
    private WebhookSecretEncryptionService encryptionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistTenantBoundWebhookGraph() {
        User actor = createActor();
        Organization organization = createOrganization(actor);
        GeneratedWebhookSecret generated = encryptionService.generate();
        EncryptedWebhookSecret encrypted = generated.encryptedSecret();

        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setOrganization(organization);
        subscription.setName("Customer integration");
        subscription.setEndpointUrl("https://example.test/crm-webhooks");
        subscription.setSecretDisplaySuffix(generated.displaySuffix());
        subscription.setCurrentSecretCiphertext(encrypted.ciphertext());
        subscription.setCurrentSecretNonce(encrypted.nonce());
        subscription.setCurrentSecretTag(encrypted.authenticationTag());
        subscription.setCurrentSecretKeyVersion(encrypted.keyVersion());
        subscription.setCreatedByUser(actor);
        subscription = subscriptionRepository.saveAndFlush(subscription);

        WebhookSubscriptionEvent subscribedEvent =
                new WebhookSubscriptionEvent();
        subscribedEvent.setId(new WebhookSubscriptionEventId(
                subscription.getId(),
                WebhookEventType.CUSTOMER_CREATED
        ));
        subscribedEvent.setSubscription(subscription);
        subscribedEvent.setOrganizationId(organization.getId());
        subscriptionEventRepository.saveAndFlush(subscribedEvent);

        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 12, 30);
        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId("evt_phase86_repository_test");
        event.setOrganization(organization);
        event.setEventType(WebhookEventType.CUSTOMER_CREATED);
        event.setAggregateType("CUSTOMER");
        event.setAggregateId(42L);
        event.setPayload("{\"customer\":{\"id\":42}}");
        event.setNextPublicationAttemptAt(now);
        event.setOccurredAt(now);
        event = eventRepository.saveAndFlush(event);

        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setPublicDeliveryId("dlv_phase86_repository_test");
        delivery.setOrganization(organization);
        delivery.setEvent(event);
        delivery.setSubscription(subscription);
        delivery.setNextAttemptAt(now);
        delivery = deliveryRepository.saveAndFlush(delivery);

        WebhookDeliveryAttempt attempt = new WebhookDeliveryAttempt();
        attempt.setOrganization(organization);
        attempt.setDelivery(delivery);
        attempt.setAttemptNumber(1);
        attempt.setOutcome(WebhookDeliveryOutcome.SUCCEEDED);
        attempt.setRequestTimestamp(1_788_699_000L);
        attempt.setHttpStatus(204);
        attempt.setDurationMs(25);
        attemptRepository.saveAndFlush(attempt);

        assertTrue(eventRepository.lockReadyForPublication(
                now.plusSeconds(1),
                now.minusMinutes(1),
                10
        ).stream().anyMatch(item -> "evt_phase86_repository_test".equals(
                item.getPublicEventId()
        )));
        assertTrue(deliveryRepository.lockReadyForDelivery(
                now.plusSeconds(1),
                now.minusMinutes(1),
                10
        ).stream().anyMatch(item -> "dlv_phase86_repository_test".equals(
                item.getPublicDeliveryId()
        )));

        delivery.setStatus(WebhookDeliveryStatus.PROCESSING);
        delivery.setAttemptCount(1);
        delivery.setClaimedAt(now.minusMinutes(5));
        delivery.setClaimToken("clm_abandoned");
        deliveryRepository.saveAndFlush(delivery);
        assertEquals(1, deliveryRepository.recoverStaleProcessing(
                now,
                now.minusMinutes(1),
                6
        ));

        Long organizationId = organization.getId();
        Long subscriptionId = subscription.getId();
        Long deliveryId = delivery.getId();
        entityManager.clear();

        WebhookSubscription loadedSubscription = subscriptionRepository
                .findByIdAndOrganizationId(subscriptionId, organizationId)
                .orElseThrow();
        assertEquals(
                generated.rawSecret(),
                encryptionService.decrypt(new EncryptedWebhookSecret(
                        loadedSubscription.getCurrentSecretCiphertext(),
                        loadedSubscription.getCurrentSecretNonce(),
                        loadedSubscription.getCurrentSecretTag(),
                        loadedSubscription.getCurrentSecretKeyVersion()
                ))
        );
        assertEquals(
                WebhookEventType.CUSTOMER_CREATED,
                subscriptionEventRepository
                        .findByOrganizationIdAndIdSubscriptionId(
                                organizationId,
                                subscriptionId
                        )
                        .getFirst()
                        .getId()
                        .getEventType()
        );
        assertEquals(
                1,
                attemptRepository
                        .findByOrganizationIdAndDeliveryIdOrderByAttemptNumberAsc(
                                organizationId,
                                deliveryId
                        )
                        .size()
        );
        assertEquals(
                WebhookDeliveryStatus.RETRY_SCHEDULED,
                deliveryRepository.findById(deliveryId)
                        .orElseThrow()
                        .getStatus()
        );
        assertTrue(subscriptionRepository.findByIdAndOrganizationId(
                subscriptionId,
                organizationId + 1_000L
        ).isEmpty());

        subscriptionEventRepository
                .deleteByOrganizationIdAndIdSubscriptionId(
                        organizationId,
                        subscriptionId
                );
        WebhookSubscriptionEvent replacementEvent =
                new WebhookSubscriptionEvent();
        replacementEvent.setId(new WebhookSubscriptionEventId(
                subscriptionId,
                WebhookEventType.TASK_COMPLETED
        ));
        replacementEvent.setSubscription(loadedSubscription);
        replacementEvent.setOrganizationId(organizationId);
        subscriptionEventRepository.saveAndFlush(replacementEvent);
        entityManager.clear();

        List<WebhookSubscriptionEvent> replacedEvents =
                subscriptionEventRepository
                        .findByOrganizationIdAndIdSubscriptionId(
                                organizationId,
                                subscriptionId
                        );
        assertEquals(1, replacedEvents.size());
        assertEquals(
                WebhookEventType.TASK_COMPLETED,
                replacedEvents.getFirst().getId().getEventType()
        );

        String storedEventType = (String) entityManager
                .createNativeQuery("""
                        SELECT event_type
                        FROM webhook_events
                        WHERE id = :eventId
                        """, String.class)
                .setParameter("eventId", event.getId())
                .getSingleResult();
        assertEquals("customer.created", storedEventType);
    }

    private User createActor() {
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User actor = new User();
        actor.setFullName("Phase 86 Webhook Administrator");
        actor.setEmail("phase86.webhook.admin@crm.test");
        actor.setPasswordHash("integration-test-password-hash");
        actor.setRole(role);
        actor.setStatus(UserStatus.ACTIVE);
        return userRepository.save(actor);
    }

    private Organization createOrganization(User actor) {
        Organization organization = new Organization();
        organization.setName("Phase 86 Organization");
        organization.setSlug("phase-86-organization");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(actor);
        return organizationRepository.save(organization);
    }
}
