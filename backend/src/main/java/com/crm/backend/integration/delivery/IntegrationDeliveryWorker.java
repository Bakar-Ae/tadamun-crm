package com.crm.backend.integration.delivery;

import com.crm.backend.observability.SaasOperationsMetrics;
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
    private final SaasOperationsMetrics metrics;

    public IntegrationDeliveryWorker(
            IntegrationDeliveryClaimService claimService,
            IntegrationDeliveryService deliveryService,
            SaasOperationsMetrics metrics
    ) {
        this.claimService = claimService;
        this.deliveryService = deliveryService;
        this.metrics = metrics;
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
            metrics.record(
                    SaasOperationsMetrics.Subsystem.INTEGRATION_DELIVERY,
                    SaasOperationsMetrics.Outcome.RECOVERED,
                    recovered
            );
            log.warn("Recovered stale integration deliveries. count={}",
                    recovered);
        }

        for (IntegrationDeliveryWorkItem item
                : claimService.claimReadyDeliveries()) {
            try {
                IntegrationDeliveryStatus status = deliveryService.deliver(
                        item.id(),
                        item.claimToken()
                );
                if (status != null) {
                    metrics.record(
                            SaasOperationsMetrics.Subsystem
                                    .INTEGRATION_DELIVERY,
                            status == IntegrationDeliveryStatus.SUCCEEDED
                                    ? SaasOperationsMetrics.Outcome.COMPLETED
                                    : SaasOperationsMetrics.Outcome.FAILED
                    );
                }
            } catch (RuntimeException failure) {
                metrics.record(
                        SaasOperationsMetrics.Subsystem.INTEGRATION_DELIVERY,
                        SaasOperationsMetrics.Outcome.FAILED
                );
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
