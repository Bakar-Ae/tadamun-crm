package com.crm.backend.workflow;

import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowActionResultService {

    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)
    );

    private final WorkflowActionExecutionRepository actionRepository;
    private final WorkflowActionAttemptRepository attemptRepository;
    private final WorkflowActionExecutor actionExecutor;
    private final WorkflowAuditService auditService;
    private final SubscriptionTimeProvider timeProvider;
    private final WorkflowWorkerProperties properties;
    private final ObjectMapper objectMapper;

    public WorkflowActionResultService(
            WorkflowActionExecutionRepository actionRepository,
            WorkflowActionAttemptRepository attemptRepository,
            WorkflowActionExecutor actionExecutor,
            WorkflowAuditService auditService,
            SubscriptionTimeProvider timeProvider,
            WorkflowWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        this.actionRepository = actionRepository;
        this.attemptRepository = attemptRepository;
        this.actionExecutor = actionExecutor;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowActionExecutionStatus processClaim(
            WorkflowQueueClaim claim
    ) {
        WorkflowActionExecution actionExecution = actionRepository
                .findForUpdate(claim.actionExecutionId())
                .orElse(null);
        if (!ownsClaim(actionExecution, claim.claimToken())) {
            return null;
        }

        long startedAt = System.nanoTime();
        try {
            WorkflowActionResult result = actionExecutor.execute(
                    actionExecution
            );
            recordSuccess(actionExecution, result, elapsedMillis(startedAt));
            return actionExecution.getStatus();
        } catch (RuntimeException failure) {
            recordFailure(actionExecution, failure, elapsedMillis(startedAt));
            return actionExecution.getStatus();
        }
    }

    @Transactional
    public boolean recoverStaleAction(Long actionExecutionId) {
        WorkflowActionExecution actionExecution = actionRepository
                .findForUpdate(actionExecutionId)
                .orElse(null);
        LocalDateTime staleBefore = timeProvider.now().minusSeconds(
                properties.getStaleClaimSeconds()
        );
        if (actionExecution == null
                || actionExecution.getStatus()
                != WorkflowActionExecutionStatus.PROCESSING
                || actionExecution.getClaimedAt() == null
                || !actionExecution.getClaimedAt().isBefore(staleBefore)) {
            return false;
        }

        IllegalStateException failure = new IllegalStateException(
                "Recovered an abandoned workflow action claim"
        );
        recordFailure(actionExecution, failure, 0);
        return true;
    }

    private void recordSuccess(
            WorkflowActionExecution actionExecution,
            WorkflowActionResult result,
            int durationMs
    ) {
        LocalDateTime now = timeProvider.now();
        actionExecution.setStatus(WorkflowActionExecutionStatus.SUCCEEDED);
        actionExecution.setResultResourceType(result.resourceType());
        actionExecution.setResultResourceId(result.resourceId());
        actionExecution.setResultSummary(writeJson(result.summary()));
        actionExecution.setLastErrorCategory(null);
        actionExecution.setLastError(null);
        actionExecution.setCompletedAt(now);
        clearClaim(actionExecution);
        saveAttempt(
                actionExecution,
                WorkflowActionAttemptOutcome.SUCCEEDED,
                durationMs,
                actionExecution.getResultSummary(),
                null,
                null
        );
        continueOrComplete(actionExecution, now);
    }

    private void recordFailure(
            WorkflowActionExecution actionExecution,
            RuntimeException failure,
            int durationMs
    ) {
        LocalDateTime now = timeProvider.now();
        boolean retryable = !(failure instanceof IllegalArgumentException);
        String category = failure.getClass().getSimpleName();
        String message = safeMessage(failure);
        int maximumAttempts = actionExecution.getExecution()
                .getWorkflow().getMaximumAttempts();
        int attemptInCycle = ((actionExecution.getAttemptCount() - 1)
                % maximumAttempts) + 1;
        boolean retry = retryable && attemptInCycle < maximumAttempts;

        saveAttempt(
                actionExecution,
                retryable
                        ? WorkflowActionAttemptOutcome.RETRYABLE_FAILURE
                        : WorkflowActionAttemptOutcome.TERMINAL_FAILURE,
                durationMs,
                null,
                category,
                message
        );
        actionExecution.setLastErrorCategory(category);
        actionExecution.setLastError(message);
        clearClaim(actionExecution);

        WorkflowExecution execution = actionExecution.getExecution();
        clearExecutionClaim(execution);
        execution.setLastErrorCategory(category);
        execution.setLastError(message);
        if (retry) {
            LocalDateTime retryAt = now.plus(retryDelay(
                    attemptInCycle
            ));
            actionExecution.setStatus(
                    WorkflowActionExecutionStatus.RETRY_SCHEDULED
            );
            actionExecution.setNextAttemptAt(retryAt);
            execution.setStatus(WorkflowExecutionStatus.RETRY_SCHEDULED);
            execution.setNextAttemptAt(retryAt);
            return;
        }

        WorkflowActionExecutionStatus actionStatus = retryable
                ? WorkflowActionExecutionStatus.DEAD
                : WorkflowActionExecutionStatus.FAILED;
        actionExecution.setStatus(actionStatus);
        actionExecution.setCompletedAt(now);
        if (retryable) {
            auditService.log(
                    actionExecution.getOrganization().getId(),
                    execution.getWorkflow().getCreatedByUser().getId(),
                    WorkflowAuditAction.WORKFLOW_EXECUTION_EXHAUSTED,
                    execution.getWorkflow().getId(),
                    auditService.details(
                            "publicExecutionId", execution.getPublicExecutionId(),
                            "actionOrder", actionExecution.getActionOrder(),
                            "attemptCount", actionExecution.getAttemptCount(),
                            "errorCategory", category
                    )
            );
        }

        if (execution.getWorkflow().getFailurePolicy()
                == WorkflowFailurePolicy.CONTINUE_ON_FAILURE) {
            continueOrComplete(actionExecution, now);
            return;
        }
        skipRemaining(actionExecution, now);
        execution.setStatus(retryable
                ? WorkflowExecutionStatus.DEAD
                : WorkflowExecutionStatus.FAILED);
        execution.setCompletedAt(now);
        execution.setCurrentActionOrder(null);
    }

    private void continueOrComplete(
            WorkflowActionExecution current,
            LocalDateTime now
    ) {
        List<WorkflowActionExecution> actions = actionRepository
                .findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
                        current.getOrganization().getId(),
                        current.getExecution().getId()
                );
        WorkflowActionExecution next = actions.stream()
                .filter(action -> action.getActionOrder()
                        > current.getActionOrder())
                .filter(action -> action.getStatus()
                        == WorkflowActionExecutionStatus.PENDING)
                .findFirst()
                .orElse(null);
        WorkflowExecution execution = current.getExecution();
        clearExecutionClaim(execution);
        if (next != null) {
            next.setNextAttemptAt(now);
            execution.setStatus(WorkflowExecutionStatus.PENDING);
            execution.setNextAttemptAt(now);
            execution.setCurrentActionOrder(next.getActionOrder());
            return;
        }

        boolean hadFailure = actions.stream().anyMatch(action ->
                action.getStatus() == WorkflowActionExecutionStatus.FAILED
                        || action.getStatus()
                        == WorkflowActionExecutionStatus.DEAD
        );
        execution.setStatus(hadFailure
                ? WorkflowExecutionStatus.PARTIALLY_SUCCEEDED
                : WorkflowExecutionStatus.SUCCEEDED);
        execution.setCurrentActionOrder(null);
        execution.setCompletedAt(now);
        execution.setLastErrorCategory(hadFailure
                ? execution.getLastErrorCategory()
                : null);
        execution.setLastError(hadFailure ? execution.getLastError() : null);
    }

    private void skipRemaining(
            WorkflowActionExecution current,
            LocalDateTime now
    ) {
        actionRepository
                .findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
                        current.getOrganization().getId(),
                        current.getExecution().getId()
                )
                .stream()
                .filter(action -> action.getActionOrder()
                        > current.getActionOrder())
                .filter(action -> action.getStatus()
                        == WorkflowActionExecutionStatus.PENDING)
                .forEach(action -> {
                    action.setStatus(WorkflowActionExecutionStatus.SKIPPED);
                    action.setNextAttemptAt(null);
                    action.setCompletedAt(now);
                });
    }

    private void saveAttempt(
            WorkflowActionExecution actionExecution,
            WorkflowActionAttemptOutcome outcome,
            int durationMs,
            String resultSummary,
            String errorCategory,
            String errorMessage
    ) {
        WorkflowActionAttempt attempt = new WorkflowActionAttempt();
        attempt.setOrganization(actionExecution.getOrganization());
        attempt.setActionExecution(actionExecution);
        attempt.setAttemptNumber(actionExecution.getAttemptCount());
        attempt.setOutcome(outcome);
        attempt.setDurationMs(durationMs);
        attempt.setResultSummary(resultSummary);
        attempt.setErrorCategory(errorCategory);
        attempt.setErrorMessage(errorMessage);
        attemptRepository.save(attempt);
    }

    private boolean ownsClaim(
            WorkflowActionExecution actionExecution,
            String claimToken
    ) {
        return actionExecution != null
                && actionExecution.getStatus()
                == WorkflowActionExecutionStatus.PROCESSING
                && claimToken != null
                && claimToken.equals(actionExecution.getClaimToken());
    }

    private Duration retryDelay(int attemptCount) {
        return RETRY_DELAYS.get(Math.min(
                Math.max(attemptCount - 1, 0),
                RETRY_DELAYS.size() - 1
        ));
    }

    private void clearClaim(WorkflowActionExecution actionExecution) {
        actionExecution.setClaimToken(null);
        actionExecution.setClaimedAt(null);
        actionExecution.setNextAttemptAt(null);
    }

    private void clearExecutionClaim(WorkflowExecution execution) {
        execution.setClaimToken(null);
        execution.setClaimedAt(null);
    }

    private int elapsedMillis(long startedAt) {
        long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
        return (int) Math.min(Math.max(elapsed, 0), Integer.MAX_VALUE);
    }

    private String safeMessage(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = "Workflow action failed";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            return "{}";
        }
    }
}
