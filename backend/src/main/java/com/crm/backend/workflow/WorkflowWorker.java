package com.crm.backend.workflow;

import com.crm.backend.observability.SaasOperationsMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.workflows.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WorkflowWorker {

    private static final Logger log = LoggerFactory.getLogger(
            WorkflowWorker.class
    );

    private final WorkflowQueueClaimService claimService;
    private final WorkflowActionResultService resultService;
    private final SaasOperationsMetrics metrics;

    public WorkflowWorker(
            WorkflowQueueClaimService claimService,
            WorkflowActionResultService resultService,
            SaasOperationsMetrics metrics
    ) {
        this.claimService = claimService;
        this.resultService = resultService;
        this.metrics = metrics;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.workflows.worker.poll-interval-ms:1000}",
            initialDelayString =
                    "${app.workflows.worker.initial-delay-ms:5000}"
    )
    public void processQueue() {
        int recovered = 0;
        for (Long actionExecutionId : claimService.findStaleActionIds()) {
            if (resultService.recoverStaleAction(actionExecutionId)) {
                recovered++;
            }
        }
        if (recovered > 0) {
            metrics.record(
                    SaasOperationsMetrics.Subsystem.WORKFLOW_ACTION,
                    SaasOperationsMetrics.Outcome.RECOVERED,
                    recovered
            );
            log.warn("Recovered stale workflow actions. count={}", recovered);
        }

        for (WorkflowQueueClaim claim : claimService.claimReadyActions()) {
            try {
                WorkflowActionExecutionStatus status =
                        resultService.processClaim(claim);
                if (status != null) {
                    boolean completed = switch (status) {
                        case SUCCEEDED, SKIPPED -> true;
                        default -> false;
                    };
                    metrics.record(
                            SaasOperationsMetrics.Subsystem.WORKFLOW_ACTION,
                            completed
                                    ? SaasOperationsMetrics.Outcome.COMPLETED
                                    : SaasOperationsMetrics.Outcome.FAILED
                    );
                }
            } catch (RuntimeException failure) {
                metrics.record(
                        SaasOperationsMetrics.Subsystem.WORKFLOW_ACTION,
                        SaasOperationsMetrics.Outcome.FAILED
                );
                log.warn(
                        "Workflow action processing failed unexpectedly. actionExecutionId={}, reason={}",
                        claim.actionExecutionId(),
                        failure.getClass().getSimpleName()
                );
            }
        }
    }
}
