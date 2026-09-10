package com.crm.backend.workflow;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.workflow.dto.WorkflowExecutionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WorkflowExecutionService {

    private static final Set<WorkflowExecutionStatus> RETRYABLE_STATUSES =
            Set.of(
                    WorkflowExecutionStatus.FAILED,
                    WorkflowExecutionStatus.DEAD,
                    WorkflowExecutionStatus.PARTIALLY_SUCCEEDED
            );
    private static final Set<WorkflowExecutionStatus> CANCELLABLE_STATUSES =
            Set.of(
                    WorkflowExecutionStatus.PENDING,
                    WorkflowExecutionStatus.RETRY_SCHEDULED
            );

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowActionExecutionRepository actionRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowExecutionMapper mapper;
    private final CurrentOrganizationProvider organizationProvider;
    private final SubscriptionFeatureAccessService featureAccessService;
    private final SubscriptionTimeProvider timeProvider;
    private final WorkflowAuditService auditService;

    public WorkflowExecutionService(
            WorkflowExecutionRepository executionRepository,
            WorkflowActionExecutionRepository actionRepository,
            WorkflowDefinitionRepository definitionRepository,
            WorkflowExecutionMapper mapper,
            CurrentOrganizationProvider organizationProvider,
            SubscriptionFeatureAccessService featureAccessService,
            SubscriptionTimeProvider timeProvider,
            WorkflowAuditService auditService
    ) {
        this.executionRepository = executionRepository;
        this.actionRepository = actionRepository;
        this.definitionRepository = definitionRepository;
        this.mapper = mapper;
        this.organizationProvider = organizationProvider;
        this.featureAccessService = featureAccessService;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    public Page<WorkflowExecutionResponse> getExecutions(Pageable pageable) {
        Long organizationId = organizationIdWithFeature();
        return executionRepository
                .findByOrganizationIdOrderByCreatedAtDesc(
                        organizationId,
                        pageable
                )
                .map(execution -> toResponse(execution, organizationId));
    }

    public Page<WorkflowExecutionResponse> getWorkflowExecutions(
            Long workflowId,
            Pageable pageable
    ) {
        Long organizationId = organizationIdWithFeature();
        requireWorkflow(workflowId, organizationId);
        return executionRepository.findByOrganizationIdAndWorkflowId(
                organizationId,
                workflowId,
                pageable
        ).map(execution -> toResponse(execution, organizationId));
    }

    public WorkflowExecutionResponse getExecution(String publicExecutionId) {
        Long organizationId = organizationIdWithFeature();
        WorkflowExecution execution = executionRepository
                .findByOrganizationIdAndPublicExecutionId(
                        organizationId,
                        publicExecutionId
                )
                .orElseThrow(this::notFound);
        return toResponse(execution, organizationId);
    }

    @Transactional
    public WorkflowExecutionResponse retryExecution(
            String publicExecutionId,
            Long actorUserId
    ) {
        Long organizationId = organizationIdWithFeature();
        WorkflowExecution execution = findForUpdate(
                publicExecutionId,
                organizationId
        );
        if (!RETRYABLE_STATUSES.contains(execution.getStatus())) {
            throw new IllegalArgumentException(
                    "Only failed workflow executions can be retried"
            );
        }

        List<WorkflowActionExecution> actions = actions(
                execution,
                organizationId
        );
        WorkflowActionExecution firstRetry = actions.stream()
                .filter(action -> action.getStatus()
                        == WorkflowActionExecutionStatus.FAILED
                        || action.getStatus()
                        == WorkflowActionExecutionStatus.DEAD)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow execution has no failed action to retry"
                ));
        LocalDateTime now = timeProvider.now();
        resetAction(firstRetry, now, true);
        actions.stream()
                .filter(action -> action.getActionOrder()
                        > firstRetry.getActionOrder())
                .filter(action -> action.getStatus()
                        == WorkflowActionExecutionStatus.SKIPPED)
                .forEach(action -> resetAction(action, now, false));

        execution.setStatus(WorkflowExecutionStatus.RETRY_SCHEDULED);
        execution.setNextAttemptAt(now);
        execution.setCurrentActionOrder(firstRetry.getActionOrder());
        execution.setClaimedAt(null);
        execution.setClaimToken(null);
        execution.setLastErrorCategory(null);
        execution.setLastError(null);
        execution.setCompletedAt(null);
        auditService.log(
                organizationId,
                actorUserId,
                WorkflowAuditAction.WORKFLOW_EXECUTION_RETRIED,
                execution.getWorkflow().getId(),
                auditService.details(
                        "publicExecutionId", publicExecutionId,
                        "actionOrder", firstRetry.getActionOrder()
                )
        );
        return mapper.toResponse(execution, actions);
    }

    @Transactional
    public WorkflowExecutionResponse cancelExecution(
            String publicExecutionId,
            Long actorUserId
    ) {
        Long organizationId = organizationIdWithFeature();
        WorkflowExecution execution = findForUpdate(
                publicExecutionId,
                organizationId
        );
        if (!CANCELLABLE_STATUSES.contains(execution.getStatus())) {
            throw new IllegalArgumentException(
                    "Only queued workflow executions can be cancelled"
            );
        }

        LocalDateTime now = timeProvider.now();
        List<WorkflowActionExecution> actions = actions(
                execution,
                organizationId
        );
        actions.stream()
                .filter(action -> action.getStatus()
                        == WorkflowActionExecutionStatus.PENDING
                        || action.getStatus()
                        == WorkflowActionExecutionStatus.RETRY_SCHEDULED)
                .forEach(action -> {
                    action.setStatus(WorkflowActionExecutionStatus.SKIPPED);
                    action.setNextAttemptAt(null);
                    action.setCompletedAt(now);
                });
        execution.setStatus(WorkflowExecutionStatus.CANCELLED);
        execution.setNextAttemptAt(now);
        execution.setCurrentActionOrder(null);
        execution.setClaimedAt(null);
        execution.setClaimToken(null);
        execution.setCompletedAt(now);
        auditService.log(
                organizationId,
                actorUserId,
                WorkflowAuditAction.WORKFLOW_EXECUTION_CANCELLED,
                execution.getWorkflow().getId(),
                auditService.details(
                        "publicExecutionId", publicExecutionId
                )
        );
        return mapper.toResponse(execution, actions);
    }

    private WorkflowExecutionResponse toResponse(
            WorkflowExecution execution,
            Long organizationId
    ) {
        return mapper.toResponse(
                execution,
                actions(execution, organizationId)
        );
    }

    private List<WorkflowActionExecution> actions(
            WorkflowExecution execution,
            Long organizationId
    ) {
        return actionRepository
                .findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
                        organizationId,
                        execution.getId()
                );
    }

    private void resetAction(
            WorkflowActionExecution action,
            LocalDateTime now,
            boolean ready
    ) {
        action.setStatus(WorkflowActionExecutionStatus.PENDING);
        action.setNextAttemptAt(ready ? now : null);
        action.setClaimedAt(null);
        action.setClaimToken(null);
        action.setLastErrorCategory(null);
        action.setLastError(null);
        action.setCompletedAt(null);
    }

    private WorkflowExecution findForUpdate(
            String publicExecutionId,
            Long organizationId
    ) {
        return executionRepository.findForUpdateByPublicId(
                organizationId,
                publicExecutionId
        ).orElseThrow(this::notFound);
    }

    private void requireWorkflow(Long workflowId, Long organizationId) {
        if (!definitionRepository.existsByIdAndOrganizationId(
                workflowId,
                organizationId
        )) {
            throw notFound();
        }
    }

    private Long organizationIdWithFeature() {
        Long organizationId = organizationProvider.getOrganizationId();
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        );
        return organizationId;
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException(
                "Workflow execution not found"
        );
    }
}
