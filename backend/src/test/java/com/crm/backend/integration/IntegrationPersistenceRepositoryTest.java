package com.crm.backend.integration;

import com.crm.backend.integration.delivery.IntegrationDelivery;
import com.crm.backend.integration.delivery.IntegrationDeliveryAttempt;
import com.crm.backend.integration.delivery.IntegrationDeliveryAttemptRepository;
import com.crm.backend.integration.delivery.IntegrationDeliveryOutcome;
import com.crm.backend.integration.delivery.IntegrationDeliveryRepository;
import com.crm.backend.integration.delivery.IntegrationDeliveryStatus;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class IntegrationPersistenceRepositoryTest {

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Autowired
    private IntegrationCredentialRepository credentialRepository;

    @Autowired
    private IntegrationDeliveryRepository deliveryRepository;

    @Autowired
    private IntegrationDeliveryAttemptRepository attemptRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldPersistAndReadConnectionWithinOwningTenant() {
        User actor = createActor();
        Organization organization = createOrganization(actor);

        IntegrationConnection connection = new IntegrationConnection();
        connection.setPublicConnectionId("int_phase88_repository_test");
        connection.setOrganization(organization);
        connection.setProvider(IntegrationProvider.WHATSAPP_CLOUD);
        connection.setName("Primary WhatsApp");
        connection.setConfiguration("{\"phoneNumberId\":\"12345\"}");
        connection.setCreatedByUser(actor);
        connection = connectionRepository.saveAndFlush(connection);

        IntegrationCredential credential = new IntegrationCredential();
        credential.setOrganization(organization);
        credential.setConnection(connection);
        credential.setCiphertext(new byte[]{1, 2, 3});
        credential.setNonce(new byte[12]);
        credential.setAuthenticationTag(new byte[16]);
        credential.setKeyVersion("v1");
        credentialRepository.saveAndFlush(credential);

        assertEquals(
                1,
                connectionRepository.findByOrganizationId(
                        organization.getId(),
                        PageRequest.of(0, 10)
                ).getTotalElements()
        );
        assertTrue(connectionRepository
                .findByPublicConnectionIdAndOrganizationId(
                        connection.getPublicConnectionId(),
                        organization.getId() + 1
                )
                .isEmpty());

        IntegrationCredential savedCredential = credentialRepository
                .findByConnectionIdAndOrganizationId(
                        connection.getId(),
                        organization.getId()
                )
                .orElseThrow();
        assertArrayEquals(
                new byte[]{1, 2, 3},
                savedCredential.getCiphertext()
        );
        assertTrue(credentialRepository
                .findByConnectionIdAndOrganizationId(
                        connection.getId(),
                        organization.getId() + 1
                )
                .isEmpty());

        IntegrationDelivery delivery = new IntegrationDelivery();
        delivery.setPublicDeliveryId("idl_phase88_repository_test");
        delivery.setOrganization(organization);
        delivery.setConnection(connection);
        delivery.setProvider(IntegrationProvider.WHATSAPP_CLOUD);
        delivery.setDeliveryType(IntegrationDeliveryType.WHATSAPP_TEXT);
        delivery.setDestination("252612345678");
        delivery.setMessageBody("Phase 88 persistence test");
        delivery.setIdempotencyKey("phase88-repository-test");
        delivery.setStatus(IntegrationDeliveryStatus.SUCCEEDED);
        delivery.setAttemptCount(1);
        delivery.setMaximumAttempts(6);
        delivery.setNextAttemptAt(java.time.LocalDateTime.now());
        delivery.setCreatedByUser(actor);
        delivery = deliveryRepository.saveAndFlush(delivery);

        IntegrationDeliveryAttempt attempt =
                new IntegrationDeliveryAttempt();
        attempt.setOrganization(organization);
        attempt.setDelivery(delivery);
        attempt.setAttemptNumber(1);
        attempt.setOutcome(IntegrationDeliveryOutcome.SUCCEEDED);
        attempt.setDurationMs(25);
        attemptRepository.saveAndFlush(attempt);

        assertEquals(
                1,
                deliveryRepository
                        .findByOrganizationIdAndConnectionIdOrderByCreatedAtDesc(
                                organization.getId(),
                                connection.getId(),
                                PageRequest.of(0, 10)
                        ).getTotalElements()
        );
        assertEquals(
                1,
                attemptRepository
                        .findByOrganizationIdAndDeliveryIdOrderByAttemptNumberAsc(
                                organization.getId(),
                                delivery.getId()
                        ).size()
        );
    }

    private User createActor() {
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User actor = new User();
        actor.setFullName("Phase 88 Integration Administrator");
        actor.setEmail("phase88.integration.admin@crm.test");
        actor.setPasswordHash("integration-test-password-hash");
        actor.setRole(role);
        actor.setStatus(UserStatus.ACTIVE);
        return userRepository.save(actor);
    }

    private Organization createOrganization(User actor) {
        Organization organization = new Organization();
        organization.setName("Phase 88 Organization");
        organization.setSlug("phase-88-organization");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(actor);
        return organizationRepository.save(organization);
    }
}
