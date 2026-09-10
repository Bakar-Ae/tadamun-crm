package com.crm.backend.workflow;

import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.webhook.WebhookEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowEventDispatchService {

    private final WorkflowTriggerRepository triggerRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowActionRepository actionRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowActionExecutionRepository actionExecutionRepository;
    private final WorkflowConditionMatcher conditionMatcher;
    private final WorkflowPublicIdGenerator idGenerator;
    private final SubscriptionTimeProvider timeProvider;

    public WorkflowEventDispatchService(
            WorkflowTriggerRepository triggerRepository,
            WorkflowDefinitionRepository definitionRepository,
            WorkflowActionRepository actionRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowActionExecutionRepository actionExecutionRepository,
            WorkflowConditionMatcher conditionMatcher,
            WorkflowPublicIdGenerator idGenerator,
            SubscriptionTimeProvider timeProvider
    ) {
        this.triggerRepository = triggerRepository;
        this.definitionRepository = definitionRepository;
        this.actionRepository = actionRepository;
        this.executionRepository = executionRepository;
        this.actionExecutionRepository = actionExecutionRepository;
        this.conditionMatcher = conditionMatcher;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int dispatch(WebhookEvent sourceEvent) {
        validateSourceEvent(sourceEvent);
        Long organizationId = sourceEvent.getOrganization().getId();
        LocalDateTime now = timeProvider.now();
        int createdExecutions = 0;

        List<WorkflowTrigger> triggers =
                triggerRepository.findMatchingTriggers(
                        organizationId,
                        sourceEvent.getEventType(),
                        WorkflowStatus.ACTIVE
                );
        for (WorkflowTrigger trigger : triggers) {
            if (!conditionMatcher.matches(
                    sourceEvent.getEventType(),
                    trigger.getConditionConfig(),
                    sourceEvent.getPayload()
            )) {
                continue;
            }

            WorkflowDefinition workflow = definitionRepository.findForUpdate(
                    trigger.getWorkflow().getId(),
                    organizationId
            ).orElse(null);
            if (!eligibleForExecution(workflow, sourceEvent, now)) {
                continue;
            }

            List<WorkflowAction> actions = actionRepository
                    .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                            organizationId,
                            workflow.getId(),
                            workflow.getDefinitionVersion()
                    )
                    .stream()
                    .filter(WorkflowAction::isEnabled)
                    .toList();
            if (actions.isEmpty()) {
                continue;
            }

            WorkflowExecution execution = newExecution(
                    workflow,
                    sourceEvent,
                    actions.getFirst().getActionOrder(),
                    now
            );
            executionRepository.save(execution);
            actionExecutionRepository.saveAll(
                    actionExecutions(execution, actions, now)
            );
            createdExecutions++;
        }
        return createdExecutions;
    }

    private boolean eligibleForExecution(
            WorkflowDefinition workflow,
            WebhookEvent sourceEvent,
            LocalDateTime now
    ) {
        if (workflow == null || workflow.getStatus() != WorkflowStatus.ACTIVE) {
            return false;
        }
        Long organizationId = sourceEvent.getOrganization().getId();
        if (executionRepository
                .existsByOrganizationIdAndWorkflowIdAndSourceEventId(
                        organizationId,
                        workflow.getId(),
                        sourceEvent.getId()
                )) {
            return false;
        }
        long recentExecutions = executionRepository
                .countByOrganizationIdAndWorkflowIdAndCreatedAtGreaterThanEqual(
                        organizationId,
                        workflow.getId(),
                        now.minusHours(1)
                );
        return recentExecutions < workflow.getMaximumExecutionsPerHour();
    }

    private WorkflowExecution newExecution(
            WorkflowDefinition workflow,
            WebhookEvent sourceEvent,
            short firstActionOrder,
            LocalDateTime now
    ) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setPublicExecutionId(idGenerator.executionId());
        execution.setOrganization(sourceEvent.getOrganization());
        execution.setWorkflow(workflow);
        execution.setSourceEvent(sourceEvent);
        execution.setWorkflowVersion(workflow.getDefinitionVersion());
        execution.setTriggerEventType(sourceEvent.getEventType());
        execution.setSourceEventPublicId(sourceEvent.getPublicEventId());
        execution.setCorrelationId(sourceEvent.getPublicEventId());
        execution.setAutomationDepth((short) 0);
        execution.setStatus(WorkflowExecutionStatus.PENDING);
        execution.setAttemptCount(0);
        execution.setNextAttemptAt(now);
        execution.setCurrentActionOrder(firstActionOrder);
        execution.setInputPayload(sourceEvent.getPayload());
        return execution;
    }

    private List<WorkflowActionExecution> actionExecutions(
            WorkflowExecution execution,
            List<WorkflowAction> actions,
            LocalDateTime now
    ) {
        return actions.stream()
                .map(action -> newActionExecution(
                        execution,
                        action,
                        action.getActionOrder()
                                == execution.getCurrentActionOrder(),
                        now
                ))
                .toList();
    }

    private WorkflowActionExecution newActionExecution(
            WorkflowExecution execution,
            WorkflowAction action,
            boolean immediatelyRunnable,
            LocalDateTime now
    ) {
        WorkflowActionExecution actionExecution =
                new WorkflowActionExecution();
        actionExecution.setPublicActionExecutionId(
                idGenerator.actionExecutionId()
        );
        actionExecution.setOrganization(execution.getOrganization());
        actionExecution.setExecution(execution);
        actionExecution.setAction(action);
        actionExecution.setActionOrder(action.getActionOrder());
        actionExecution.setActionType(action.getActionType());
        actionExecution.setConfigurationSnapshot(action.getConfiguration());
        actionExecution.setStatus(WorkflowActionExecutionStatus.PENDING);
        actionExecution.setAttemptCount(0);
        actionExecution.setNextAttemptAt(
                immediatelyRunnable ? now : null
        );
        actionExecution.setIdempotencyKey(
                execution.getPublicExecutionId()
                        + ":"
                        + action.getPublicActionId()
        );
        return actionExecution;
    }

    private void validateSourceEvent(WebhookEvent sourceEvent) {
        if (sourceEvent == null
                || sourceEvent.getId() == null
                || sourceEvent.getOrganization() == null
                || sourceEvent.getOrganization().getId() == null
                || sourceEvent.getEventType() == null
                || sourceEvent.getPublicEventId() == null
                || sourceEvent.getPublicEventId().isBlank()
                || sourceEvent.getPayload() == null
                || sourceEvent.getPayload().isBlank()) {
            throw new IllegalArgumentException(
                    "A persisted workflow source event is required"
            );
        }
    }
}
