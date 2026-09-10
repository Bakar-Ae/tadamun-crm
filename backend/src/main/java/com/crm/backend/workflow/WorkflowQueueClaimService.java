package com.crm.backend.workflow;

import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowQueueClaimService {

    private static final List<WorkflowActionExecutionStatus> READY_STATUSES =
            List.of(
                    WorkflowActionExecutionStatus.PENDING,
                    WorkflowActionExecutionStatus.RETRY_SCHEDULED
            );

    private final WorkflowActionExecutionRepository repository;
    private final WorkflowPublicIdGenerator idGenerator;
    private final SubscriptionTimeProvider timeProvider;
    private final WorkflowWorkerProperties properties;

    public WorkflowQueueClaimService(
            WorkflowActionExecutionRepository repository,
            WorkflowPublicIdGenerator idGenerator,
            SubscriptionTimeProvider timeProvider,
            WorkflowWorkerProperties properties
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.properties = properties;
    }

    @Transactional
    public List<WorkflowQueueClaim> claimReadyActions() {
        LocalDateTime now = timeProvider.now();
        List<Long> ids = repository.findClaimableIds(
                READY_STATUSES,
                now,
                PageRequest.of(0, properties.getBatchSize())
        );
        List<WorkflowQueueClaim> claims = new ArrayList<>();
        for (Long id : ids) {
            WorkflowActionExecution actionExecution = repository
                    .findForUpdate(id)
                    .orElse(null);
            if (actionExecution == null
                    || !READY_STATUSES.contains(actionExecution.getStatus())
                    || actionExecution.getNextAttemptAt() == null
                    || actionExecution.getNextAttemptAt().isAfter(now)) {
                continue;
            }
            String claimToken = idGenerator.claimToken();
            actionExecution.setStatus(WorkflowActionExecutionStatus.PROCESSING);
            actionExecution.setAttemptCount(
                    actionExecution.getAttemptCount() + 1
            );
            actionExecution.setClaimedAt(now);
            actionExecution.setClaimToken(claimToken);
            if (actionExecution.getStartedAt() == null) {
                actionExecution.setStartedAt(now);
            }

            WorkflowExecution execution = actionExecution.getExecution();
            execution.setStatus(WorkflowExecutionStatus.PROCESSING);
            execution.setAttemptCount(execution.getAttemptCount() + 1);
            execution.setClaimedAt(now);
            execution.setClaimToken(claimToken);
            execution.setNextAttemptAt(now);
            if (execution.getStartedAt() == null) {
                execution.setStartedAt(now);
            }
            claims.add(new WorkflowQueueClaim(id, claimToken));
        }
        return List.copyOf(claims);
    }

    public List<Long> findStaleActionIds() {
        return repository.findStaleIds(
                WorkflowActionExecutionStatus.PROCESSING,
                timeProvider.now().minusSeconds(
                        properties.getStaleClaimSeconds()
                ),
                PageRequest.of(0, properties.getBatchSize())
        );
    }
}
