package com.crm.backend.integration.delivery;

import com.crm.backend.integration.IntegrationAuditService;
import com.crm.backend.integration.IntegrationConnection;
import com.crm.backend.integration.IntegrationConnectionService;
import com.crm.backend.integration.IntegrationCredentialService;
import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.IntegrationPublicIdGenerator;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
import com.crm.backend.integration.provider.IntegrationProviderAdapter;
import com.crm.backend.integration.provider.IntegrationProviderContext;
import com.crm.backend.integration.provider.IntegrationProviderDeliveryResult;
import com.crm.backend.integration.provider.IntegrationProviderException;
import com.crm.backend.integration.provider.IntegrationProviderRegistry;
import com.crm.backend.integration.provider.IntegrationSecrets;
import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationDeliveryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 9, 10, 12, 0
    );

    @Mock
    private IntegrationDeliveryRepository deliveryRepository;
    @Mock
    private IntegrationDeliveryAttemptRepository attemptRepository;
    @Mock
    private IntegrationConnectionService connectionService;
    @Mock
    private IntegrationCredentialService credentialService;
    @Mock
    private IntegrationProviderRegistry providerRegistry;
    @Mock
    private IntegrationPublicIdGenerator idGenerator;
    @Mock
    private IntegrationDeliveryMapper mapper;
    @Mock
    private IntegrationDeliveryWorkerProperties workerProperties;
    @Mock
    private IntegrationAuditService auditService;
    @Mock
    private CurrentOrganizationProvider organizationProvider;
    @Mock
    private SubscriptionTimeProvider timeProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IntegrationProviderAdapter adapter;

    private IntegrationDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationDeliveryService(
                deliveryRepository,
                attemptRepository,
                connectionService,
                credentialService,
                providerRegistry,
                idGenerator,
                mapper,
                new IntegrationDeliveryRetryPolicy(),
                workerProperties,
                auditService,
                organizationProvider,
                timeProvider,
                userRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldRecordSuccessfulProviderDelivery() {
        IntegrationDelivery delivery = processingDelivery();
        prepareDelivery(delivery);
        when(adapter.deliver(any(IntegrationProviderContext.class), any()))
                .thenReturn(new IntegrationProviderDeliveryResult(
                        "provider-message-1"
                ));

        service.deliver(delivery.getId(), "claim-1");

        assertEquals(IntegrationDeliveryStatus.SUCCEEDED, delivery.getStatus());
        assertEquals("provider-message-1", delivery.getProviderMessageId());
        assertEquals(NOW, delivery.getCompletedAt());
        assertNull(delivery.getClaimToken());
        assertNull(delivery.getClaimedAt());

        ArgumentCaptor<IntegrationDeliveryAttempt> attempt =
                ArgumentCaptor.forClass(IntegrationDeliveryAttempt.class);
        verify(attemptRepository).save(attempt.capture());
        assertEquals(
                IntegrationDeliveryOutcome.SUCCEEDED,
                attempt.getValue().getOutcome()
        );
        assertEquals(1, attempt.getValue().getAttemptNumber());
    }

    @Test
    void shouldScheduleRetryForTemporaryProviderFailure() {
        IntegrationDelivery delivery = processingDelivery();
        prepareDelivery(delivery);
        when(adapter.deliver(any(IntegrationProviderContext.class), any()))
                .thenThrow(new IntegrationProviderException(
                        "HTTP_503",
                        "Provider unavailable",
                        true
                ));

        service.deliver(delivery.getId(), "claim-1");

        assertEquals(
                IntegrationDeliveryStatus.RETRY_SCHEDULED,
                delivery.getStatus()
        );
        assertEquals(NOW.plusMinutes(1), delivery.getNextAttemptAt());
        assertEquals("HTTP_503", delivery.getLastErrorCategory());
        assertNull(delivery.getClaimToken());

        ArgumentCaptor<IntegrationDeliveryAttempt> attempt =
                ArgumentCaptor.forClass(IntegrationDeliveryAttempt.class);
        verify(attemptRepository).save(attempt.capture());
        assertEquals(
                IntegrationDeliveryOutcome.RETRYABLE_FAILURE,
                attempt.getValue().getOutcome()
        );
    }

    private void prepareDelivery(IntegrationDelivery delivery) {
        when(deliveryRepository.findForWorkerUpdate(delivery.getId()))
                .thenReturn(Optional.of(delivery));
        when(providerRegistry.require(IntegrationProvider.SMTP))
                .thenReturn(adapter);
        when(credentialService.load(delivery.getConnection()))
                .thenReturn(new IntegrationSecrets(Map.of()));
        when(timeProvider.now()).thenReturn(NOW);
    }

    private IntegrationDelivery processingDelivery() {
        Organization organization = new Organization();
        organization.setId(1L);
        User actor = new User();
        actor.setId(9L);
        IntegrationConnection connection = new IntegrationConnection();
        connection.setId(3L);
        connection.setOrganization(organization);
        connection.setPublicConnectionId("int_test");
        connection.setProvider(IntegrationProvider.SMTP);
        connection.setConfiguration("{}");

        IntegrationDelivery delivery = new IntegrationDelivery();
        delivery.setId(5L);
        delivery.setOrganization(organization);
        delivery.setConnection(connection);
        delivery.setProvider(IntegrationProvider.SMTP);
        delivery.setDeliveryType(IntegrationDeliveryType.EMAIL);
        delivery.setDestination("person@example.com");
        delivery.setSubject("Test");
        delivery.setMessageBody("Hello");
        delivery.setIdempotencyKey("test-key");
        delivery.setStatus(IntegrationDeliveryStatus.PROCESSING);
        delivery.setAttemptCount(1);
        delivery.setMaximumAttempts(6);
        delivery.setClaimToken("claim-1");
        delivery.setClaimedAt(NOW.minusSeconds(1));
        delivery.setCreatedByUser(actor);
        return delivery;
    }
}
