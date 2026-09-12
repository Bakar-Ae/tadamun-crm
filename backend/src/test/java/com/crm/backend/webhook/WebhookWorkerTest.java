package com.crm.backend.webhook;

import com.crm.backend.observability.SaasOperationsMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookWorkerTest {

    @Test
    void oneFailedItemShouldNotStopRemainingQueueWork() {
        WebhookQueueClaimService claimService = mock(
                WebhookQueueClaimService.class
        );
        WebhookEventFanoutService fanoutService = mock(
                WebhookEventFanoutService.class
        );
        WebhookDeliveryService deliveryService = mock(
                WebhookDeliveryService.class
        );
        SaasOperationsMetrics metrics = mock(SaasOperationsMetrics.class);
        WebhookWorkItem failedEvent = new WebhookWorkItem(1L, "clm_1");
        WebhookWorkItem nextEvent = new WebhookWorkItem(2L, "clm_2");
        WebhookWorkItem failedDelivery = new WebhookWorkItem(3L, "clm_3");
        when(claimService.claimEvents()).thenReturn(List.of(
                failedEvent,
                nextEvent
        ));
        when(claimService.claimDeliveries()).thenReturn(List.of(
                failedDelivery
        ));
        when(fanoutService.fanOut(1L, "clm_1"))
                .thenThrow(new IllegalStateException("event failed"));
        when(deliveryService.deliver(3L, "clm_3"))
                .thenThrow(new IllegalStateException("delivery failed"));

        new WebhookWorker(
                claimService,
                fanoutService,
                deliveryService,
                metrics
        ).processQueues();

        verify(claimService).recordEventFailure(
                eq(failedEvent),
                any(IllegalStateException.class)
        );
        verify(fanoutService).fanOut(2L, "clm_2");
        verify(claimService).releaseDeliveryAfterWorkerFailure(
                eq(failedDelivery),
                any(IllegalStateException.class)
        );
        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.WEBHOOK_EVENT,
                SaasOperationsMetrics.Outcome.COMPLETED
        );
        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.WEBHOOK_EVENT,
                SaasOperationsMetrics.Outcome.FAILED
        );
        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                SaasOperationsMetrics.Outcome.FAILED
        );
    }
}
