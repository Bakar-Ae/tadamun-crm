package com.crm.backend.integration.delivery;

import com.crm.backend.integration.IntegrationPublicIdGenerator;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class IntegrationDeliveryClaimService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final IntegrationDeliveryRepository repository;
    private final IntegrationPublicIdGenerator idGenerator;
    private final SubscriptionTimeProvider timeProvider;
    private final IntegrationDeliveryWorkerProperties properties;
    private final IntegrationDeliveryRetryPolicy retryPolicy;

    public IntegrationDeliveryClaimService(
            IntegrationDeliveryRepository repository,
            IntegrationPublicIdGenerator idGenerator,
            SubscriptionTimeProvider timeProvider,
            IntegrationDeliveryWorkerProperties properties,
            IntegrationDeliveryRetryPolicy retryPolicy
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.properties = properties;
        this.retryPolicy = retryPolicy;
    }

    @Transactional
    public List<IntegrationDeliveryWorkItem> claimReadyDeliveries() {
        LocalDateTime now = timeProvider.now();
        return repository.lockReadyForDelivery(
                now,
                staleBefore(now),
                properties.getBatchSize()
        ).stream().map(delivery -> {
            String token = idGenerator.claimToken();
            delivery.setStatus(IntegrationDeliveryStatus.PROCESSING);
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setClaimedAt(now);
            delivery.setClaimToken(token);
            return new IntegrationDeliveryWorkItem(delivery.getId(), token);
        }).toList();
    }

    @Transactional
    public int recoverStaleDeliveries() {
        LocalDateTime now = timeProvider.now();
        return repository.recoverStaleProcessing(now, staleBefore(now));
    }

    @Transactional
    public void releaseAfterWorkerFailure(
            IntegrationDeliveryWorkItem item,
            RuntimeException failure
    ) {
        repository.findForWorkerUpdate(item.id())
                .filter(delivery -> Objects.equals(
                        item.claimToken(), delivery.getClaimToken()
                ))
                .filter(delivery -> delivery.getStatus()
                        == IntegrationDeliveryStatus.PROCESSING)
                .ifPresent(delivery -> {
                    LocalDateTime now = timeProvider.now();
                    delivery.setClaimedAt(null);
                    delivery.setClaimToken(null);
                    delivery.setLastErrorCategory("WORKER_FAILURE");
                    delivery.setLastErrorMessage(safeMessage(failure));
                    if (delivery.getAttemptCount()
                            >= delivery.getMaximumAttempts()) {
                        delivery.setStatus(IntegrationDeliveryStatus.DEAD);
                        delivery.setCompletedAt(now);
                    } else {
                        delivery.setStatus(
                                IntegrationDeliveryStatus.RETRY_SCHEDULED
                        );
                        delivery.setNextAttemptAt(retryPolicy.nextAttemptAt(
                                delivery.getAttemptCount(), now
                        ));
                    }
                });
    }

    private LocalDateTime staleBefore(LocalDateTime now) {
        return now.minusSeconds(properties.getStaleClaimSeconds());
    }

    private String safeMessage(RuntimeException failure) {
        String message = failure.getClass().getSimpleName();
        return message.length() <= MAX_ERROR_LENGTH
                ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
