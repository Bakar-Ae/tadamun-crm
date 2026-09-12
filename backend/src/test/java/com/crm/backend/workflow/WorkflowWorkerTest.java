package com.crm.backend.workflow;

import com.crm.backend.observability.SaasOperationsMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowWorkerTest {

    @Test
    void workerShouldRecordActualActionOutcomes() {
        WorkflowQueueClaimService claimService = mock(
                WorkflowQueueClaimService.class
        );
        WorkflowActionResultService resultService = mock(
                WorkflowActionResultService.class
        );
        SaasOperationsMetrics metrics = mock(SaasOperationsMetrics.class);
        WorkflowQueueClaim succeeded = new WorkflowQueueClaim(1L, "clm_1");
        WorkflowQueueClaim retrying = new WorkflowQueueClaim(2L, "clm_2");

        when(claimService.findStaleActionIds()).thenReturn(List.of());
        when(claimService.claimReadyActions()).thenReturn(List.of(
                succeeded,
                retrying
        ));
        when(resultService.processClaim(succeeded)).thenReturn(
                WorkflowActionExecutionStatus.SUCCEEDED
        );
        when(resultService.processClaim(retrying)).thenReturn(
                WorkflowActionExecutionStatus.RETRY_SCHEDULED
        );

        new WorkflowWorker(claimService, resultService, metrics)
                .processQueue();

        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.WORKFLOW_ACTION,
                SaasOperationsMetrics.Outcome.COMPLETED
        );
        verify(metrics).record(
                SaasOperationsMetrics.Subsystem.WORKFLOW_ACTION,
                SaasOperationsMetrics.Outcome.FAILED
        );
    }
}
