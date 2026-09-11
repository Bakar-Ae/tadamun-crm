package com.crm.backend.integration.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.integrations.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IntegrationDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(
            IntegrationDeliveryWorker.class
    );

    private final IntegrationDeliveryClaimService claimService;
    private final IntegrationDeliveryService deliveryService;

    public IntegrationDeliveryWorker(
            IntegrationDeliveryClaimService claimService,
            IntegrationDeliveryService deliveryService
    ) {
        this.claimService = claimService;
        this.deliveryService = deliveryService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.integrations.worker.poll-interval-ms:1000}",
            initialDelayString =
                    "${app.integrations.worker.initial-delay-ms:5000}"
    )
    public void processQueue() {
        int recovered = claimService.recoverStaleDeliveries();
        if (recovered > 0) {
            log.warn("Recovered stale integration deliveries. count={}",
                    recovered);
        }

        for (IntegrationDeliveryWorkItem item
                : claimService.claimReadyDeliveries()) {
            try {
                deliveryService.deliver(item.id(), item.claimToken());
            } catch (RuntimeException failure) {
                log.warn(
                        "Integration delivery worker failed. deliveryId={}, reason={}",
                        item.id(),
                        failure.getClass().getSimpleName()
                );
                claimService.releaseAfterWorkerFailure(item, failure);
            }
        }
    }
}
