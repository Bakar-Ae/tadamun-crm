package com.crm.backend.webhook;

import com.crm.backend.observability.SaasOperationsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "app.webhooks.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WebhookWorker {

    private static final Logger log = LoggerFactory.getLogger(
            WebhookWorker.class
    );

    private final WebhookQueueClaimService claimService;
    private final WebhookEventFanoutService fanoutService;
    private final WebhookDeliveryService deliveryService;
    private final SaasOperationsMetrics metrics;

    public WebhookWorker(
            WebhookQueueClaimService claimService,
            WebhookEventFanoutService fanoutService,
            WebhookDeliveryService deliveryService,
            SaasOperationsMetrics metrics
    ) {
        this.claimService = claimService;
        this.fanoutService = fanoutService;
        this.deliveryService = deliveryService;
        this.metrics = metrics;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.webhooks.worker.poll-interval-ms:1000}",
            initialDelayString =
                    "${app.webhooks.worker.initial-delay-ms:5000}"
    )
    public void processQueues() {
        int recovered = claimService.recoverStaleDeliveries();
        if (recovered > 0) {
            metrics.record(
                    SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                    SaasOperationsMetrics.Outcome.RECOVERED,
                    recovered
            );
            log.warn("Recovered stale webhook deliveries. count={}",
                    recovered);
        }

        processEvents(claimService.claimEvents());
        processDeliveries(claimService.claimDeliveries());
    }

    private void processEvents(List<WebhookWorkItem> items) {
        for (WebhookWorkItem item : items) {
            try {
                fanoutService.fanOut(item.id(), item.claimToken());
                metrics.record(
                        SaasOperationsMetrics.Subsystem.WEBHOOK_EVENT,
                        SaasOperationsMetrics.Outcome.COMPLETED
                );
            } catch (RuntimeException failure) {
                metrics.record(
                        SaasOperationsMetrics.Subsystem.WEBHOOK_EVENT,
                        SaasOperationsMetrics.Outcome.FAILED
                );
                log.warn(
                        "Webhook event fan-out failed. eventId={}, reason={}",
                        item.id(),
                        failure.getClass().getSimpleName()
                );
                claimService.recordEventFailure(item, failure);
            }
        }
    }

    private void processDeliveries(List<WebhookWorkItem> items) {
        for (WebhookWorkItem item : items) {
            try {
                WebhookDeliveryOutcome outcome = deliveryService.deliver(
                        item.id(),
                        item.claimToken()
                );
                metrics.record(
                        SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                        outcome == WebhookDeliveryOutcome.SUCCEEDED
                                ? SaasOperationsMetrics.Outcome.COMPLETED
                                : SaasOperationsMetrics.Outcome.FAILED
                );
            } catch (RuntimeException failure) {
                metrics.record(
                        SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                        SaasOperationsMetrics.Outcome.FAILED
                );
                log.warn(
                        "Webhook delivery worker failed. deliveryId={}, reason={}",
                        item.id(),
                        failure.getClass().getSimpleName()
                );
                claimService.releaseDeliveryAfterWorkerFailure(
                        item,
                        failure
                );
            }
        }
    }
}
