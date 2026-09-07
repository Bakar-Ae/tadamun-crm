package com.crm.backend.webhook;

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

    public WebhookWorker(
            WebhookQueueClaimService claimService,
            WebhookEventFanoutService fanoutService,
            WebhookDeliveryService deliveryService
    ) {
        this.claimService = claimService;
        this.fanoutService = fanoutService;
        this.deliveryService = deliveryService;
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
            } catch (RuntimeException failure) {
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
                deliveryService.deliver(item.id(), item.claimToken());
            } catch (RuntimeException failure) {
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
