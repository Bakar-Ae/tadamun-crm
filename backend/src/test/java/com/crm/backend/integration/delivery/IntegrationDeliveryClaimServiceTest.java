package com.crm.backend.integration.delivery;

import com.crm.backend.integration.IntegrationPublicIdGenerator;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationDeliveryClaimServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 9, 11, 9, 0
    );

    @Mock
    private IntegrationDeliveryRepository repository;

    @Mock
    private IntegrationPublicIdGenerator idGenerator;

    @Mock
    private SubscriptionTimeProvider timeProvider;

    private IntegrationDeliveryWorkerProperties properties;
    private IntegrationDeliveryClaimService service;

    @BeforeEach
    void setUp() {
        properties = new IntegrationDeliveryWorkerProperties();
        when(timeProvider.now()).thenReturn(NOW);
        service = new IntegrationDeliveryClaimService(
                repository,
                idGenerator,
                timeProvider,
                properties,
                new IntegrationDeliveryRetryPolicy()
        );
    }

    @Test
    void shouldClaimReadyDeliveryWithOwnershipToken() {
        IntegrationDelivery delivery = delivery(1, 6);
        delivery.setId(42L);
        delivery.setStatus(IntegrationDeliveryStatus.PENDING);
        when(repository.lockReadyForDelivery(
                NOW,
                NOW.minusSeconds(120),
                10
        )).thenReturn(List.of(delivery));
        when(idGenerator.claimToken()).thenReturn("claim-phase89");

        List<IntegrationDeliveryWorkItem> work =
                service.claimReadyDeliveries();

        assertEquals(
                List.of(new IntegrationDeliveryWorkItem(
                        42L,
                        "claim-phase89"
                )),
                work
        );
        assertEquals(IntegrationDeliveryStatus.PROCESSING,
                delivery.getStatus());
        assertEquals(2, delivery.getAttemptCount());
        assertEquals(NOW, delivery.getClaimedAt());
        assertEquals("claim-phase89", delivery.getClaimToken());
    }

    @Test
    void shouldDelegateStaleClaimRecoveryWithConfiguredBoundary() {
        when(repository.recoverStaleProcessing(
                NOW,
                NOW.minusSeconds(120)
        )).thenReturn(3);

        assertEquals(3, service.recoverStaleDeliveries());
        verify(repository).recoverStaleProcessing(
                NOW,
                NOW.minusSeconds(120)
        );
    }

    @Test
    void shouldReleaseUnexpectedWorkerFailureIntoRetryQueue() {
        IntegrationDelivery delivery = delivery(1, 6);
        when(repository.findForWorkerUpdate(42L))
                .thenReturn(Optional.of(delivery));

        service.releaseAfterWorkerFailure(
                new IntegrationDeliveryWorkItem(42L, "claim-phase89"),
                new IllegalStateException("private failure detail")
        );

        assertEquals(
                IntegrationDeliveryStatus.RETRY_SCHEDULED,
                delivery.getStatus()
        );
        assertEquals(NOW.plusMinutes(1), delivery.getNextAttemptAt());
        assertEquals("WORKER_FAILURE", delivery.getLastErrorCategory());
        assertEquals(
                "IllegalStateException",
                delivery.getLastErrorMessage()
        );
        assertNull(delivery.getClaimedAt());
        assertNull(delivery.getClaimToken());
    }

    @Test
    void shouldDeadLetterUnexpectedFailureAfterFinalAttempt() {
        IntegrationDelivery delivery = delivery(6, 6);
        when(repository.findForWorkerUpdate(42L))
                .thenReturn(Optional.of(delivery));

        service.releaseAfterWorkerFailure(
                new IntegrationDeliveryWorkItem(42L, "claim-phase89"),
                new IllegalStateException("private failure detail")
        );

        assertEquals(IntegrationDeliveryStatus.DEAD, delivery.getStatus());
        assertEquals(NOW, delivery.getCompletedAt());
        assertNull(delivery.getClaimedAt());
        assertNull(delivery.getClaimToken());
    }

    private IntegrationDelivery delivery(int attempts, int maximumAttempts) {
        IntegrationDelivery delivery = new IntegrationDelivery();
        delivery.setId(42L);
        delivery.setStatus(IntegrationDeliveryStatus.PROCESSING);
        delivery.setAttemptCount(attempts);
        delivery.setMaximumAttempts(maximumAttempts);
        delivery.setClaimedAt(NOW.minusMinutes(5));
        delivery.setClaimToken("claim-phase89");
        return delivery;
    }
}
