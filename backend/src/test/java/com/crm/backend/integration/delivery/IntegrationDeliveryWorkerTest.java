package com.crm.backend.integration.delivery;

import com.crm.backend.observability.SaasOperationsMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationDeliveryWorkerTest {

    @Test
    void workerShouldRecordActualDeliveryOutcomes() {
        IntegrationDeliveryClaimService claimService = mock(
                IntegrationDeliveryClaimService.class
        );
        IntegrationDeliveryService deliveryService = mock(
                IntegrationDeliveryService.class
        );
        SaasOperationsMetrics metrics = mock(SaasOperationsMetrics.class);
        IntegrationDeliveryWorkItem succeeded =
                new IntegrationDeliveryWorkItem(1L, "clm_1");
        IntegrationDeliveryWorkItem retrying =
                new IntegrationDeliveryWorkItem(2L, "clm_2");

        when(claimService.claimReadyDeliveries()).thenReturn(List.of(
                succeeded,
                retrying
        ));
        when(deliveryService.deliver(1L, "clm_1")).thenReturn(
                IntegrationDeliveryStatus.SUCCEEDED
        );
        when(deliveryService.deliver(2L, "clm_2")).thenReturn(
                IntegrationDeliveryStatus.RETRY_SCHEDULED
        );

        new IntegrationDeliveryWorker(claimService, deliveryService, metrics)
                .processQueue();

        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.INTEGRATION_DELIVERY,
                SaasOperationsMetrics.Outcome.COMPLETED
        );
        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.INTEGRATION_DELIVERY,
                SaasOperationsMetrics.Outcome.FAILED
        );
    }
}
