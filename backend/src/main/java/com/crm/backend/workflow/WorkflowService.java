package com.crm.backend.workflow;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.SubscriptionFeatureAccessService;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.subscription.usage.SubscriptionLimitExceededException;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.webhook.WebhookEventType;
import com.crm.backend.workflow.dto.CreateWorkflowRequest;
import com.crm.backend.workflow.dto.UpdateWorkflowRequest;
import com.crm.backend.workflow.dto.WorkflowActionRequest;
import com.crm.backend.workflow.dto.WorkflowResponse;
import com.crm.backend.workflow.dto.WorkflowTriggerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkflowService {

    private static final int DEFAULT_EXECUTION_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAXIMUM_ATTEMPTS = 3;
    private static final int DEFAULT_MAXIMUM_EXECUTIONS_PER_HOUR = 100;
    private static final int DEFAULT_ACTION_TIMEOUT_SECONDS = 15;

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowTriggerRepository triggerRepository;
    private final WorkflowActionRepository actionRepository;
    private final WorkflowConfigurationValidator configurationValidator;
    private final WorkflowMapper mapper;
    private final WorkflowPublicIdGenerator idGenerator;
    private final CurrentOrganizationProvider organizationProvider;
    private final UserRepository userRepository;
    private final SubscriptionFeatureAccessService featureAccessService;
    private final SubscriptionTimeProvider timeProvider;
    private final WorkflowAuditService auditService;

    public WorkflowService(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowTriggerRepository triggerRepository,
            WorkflowActionRepository actionRepository,
            WorkflowConfigurationValidator configurationValidator,
            WorkflowMapper mapper,
            WorkflowPublicIdGenerator idGenerator,
            CurrentOrganizationProvider organizationProvider,
            UserRepository userRepository,
            SubscriptionFeatureAccessService featureAccessService,
            SubscriptionTimeProvider timeProvider,
            WorkflowAuditService auditService
    ) {
        this.definitionRepository = definitionRepository;
        this.triggerRepository = triggerRepository;
        this.actionRepository = actionRepository;
        this.configurationValidator = configurationValidator;
        this.mapper = mapper;
        this.idGenerator = idGenerator;
        this.organizationProvider = organizationProvider;
        this.userRepository = userRepository;
        this.featureAccessService = featureAccessService;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
    }

    public Page<WorkflowResponse> getWorkflows(Pageable pageable) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        Page<WorkflowDefinition> workflows = definitionRepository
                .findByOrganizationId(organizationId, pageable);
        WorkflowStructures structures = loadStructures(
                organizationId,
                workflows.getContent()
        );
        return workflows.map(workflow -> mapper.toResponse(
                workflow,
                structures.triggers().get(workflow.getId()),
                structures.actions().getOrDefault(workflow.getId(), List.of())
        ));
    }

    public WorkflowResponse getWorkflow(Long id) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WorkflowDefinition workflow = definitionRepository
                .findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(this::notFound);
        return toResponse(workflow, organizationId);
    }

    @Transactional
    public WorkflowResponse createWorkflow(
            CreateWorkflowRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        String name = normalizeName(request.name());
        requireUniqueName(organizationId, name, null);

        User actor = userRepository.getReferenceById(actorUserId);
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setPublicWorkflowId(idGenerator.workflowId());
        workflow.setOrganization(organizationProvider.getOrganizationReference());
        workflow.setStatus(WorkflowStatus.DRAFT);
        workflow.setCreatedByUser(actor);
        applyDefinition(
                workflow,
                name,
                request.description(),
                request.failurePolicy(),
                request.executionTimeoutSeconds(),
                request.maximumAttempts(),
                request.maximumExecutionsPerHour()
        );
        WorkflowDefinition saved = definitionRepository.saveAndFlush(workflow);
        WorkflowStructure structure = saveStructure(
                saved,
                organizationId,
                request.trigger(),
                request.actions()
        );
        auditService.log(
                organizationId,
                actorUserId,
                WorkflowAuditAction.WORKFLOW_CREATED,
                saved.getId(),
                auditService.details(
                        "publicWorkflowId", saved.getPublicWorkflowId(),
                        "name", saved.getName(),
                        "status", saved.getStatus().name(),
                        "eventType", structure.trigger().getEventType().getValue(),
                        "actionCount", structure.actions().size()
                )
        );
        return mapper.toResponse(
                saved,
                structure.trigger(),
                structure.actions()
        );
    }

    @Transactional
    public WorkflowResponse updateWorkflow(
            Long id,
            UpdateWorkflowRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WorkflowDefinition workflow = findForUpdate(id, organizationId);
        requireEditable(workflow);
        String name = normalizeName(request.name());
        requireUniqueName(organizationId, name, id);

        LocalDateTime now = timeProvider.now();
        List<WorkflowAction> previousActions = currentActions(
                workflow,
                organizationId
        );
        previousActions.forEach(action -> action.setRetiredAt(now));
        actionRepository.saveAll(previousActions);

        workflow.setDefinitionVersion(workflow.getDefinitionVersion() + 1);
        workflow.setUpdatedByUser(userRepository.getReferenceById(actorUserId));
        applyDefinition(
                workflow,
                name,
                request.description(),
                request.failurePolicy(),
                request.executionTimeoutSeconds(),
                request.maximumAttempts(),
                request.maximumExecutionsPerHour()
        );
        WorkflowDefinition saved = definitionRepository.saveAndFlush(workflow);
        WorkflowStructure structure = saveStructure(
                saved,
                organizationId,
                request.trigger(),
                request.actions()
        );
        auditService.log(
                organizationId,
                actorUserId,
                WorkflowAuditAction.WORKFLOW_UPDATED,
                saved.getId(),
                auditService.details(
                        "publicWorkflowId", saved.getPublicWorkflowId(),
                        "name", saved.getName(),
                        "definitionVersion", saved.getDefinitionVersion(),
                        "eventType", structure.trigger().getEventType().getValue(),
                        "actionCount", structure.actions().size()
                )
        );
        return mapper.toResponse(
                saved,
                structure.trigger(),
                structure.actions()
        );
    }

    @Transactional
    public WorkflowResponse activateWorkflow(Long id, Long actorUserId) {
        Long organizationId = organizationProvider.getOrganizationId();
        Long limit = featureAccessService.requireFeatureAndGetLimitForUpdate(
                organizationId,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        );
        WorkflowDefinition workflow = findForUpdate(id, organizationId);
        if (workflow.getStatus() == WorkflowStatus.ACTIVE) {
            return toResponse(workflow, organizationId);
        }
        if (workflow.getStatus() == WorkflowStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "An archived workflow cannot be activated"
            );
        }

        long activeCount = definitionRepository.countByOrganizationIdAndStatus(
                organizationId,
                WorkflowStatus.ACTIVE
        );
        if (limit != null && activeCount >= limit) {
            throw new SubscriptionLimitExceededException(
                    SubscriptionFeature.WORKFLOW_AUTOMATION,
                    activeCount,
                    limit
            );
        }
        WorkflowTrigger trigger = currentTrigger(workflow, organizationId);
        List<WorkflowAction> actions = currentActions(workflow, organizationId);
        validateActivation(trigger, actions);

        workflow.setStatus(WorkflowStatus.ACTIVE);
        if (workflow.getActivatedAt() == null) {
            workflow.setActivatedAt(timeProvider.now());
        }
        workflow.setUpdatedByUser(userRepository.getReferenceById(actorUserId));
        WorkflowDefinition saved = definitionRepository.saveAndFlush(workflow);
        auditLifecycle(
                organizationId,
                actorUserId,
                saved,
                WorkflowAuditAction.WORKFLOW_ACTIVATED
        );
        return mapper.toResponse(saved, trigger, actions);
    }

    @Transactional
    public WorkflowResponse pauseWorkflow(Long id, Long actorUserId) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WorkflowDefinition workflow = findForUpdate(id, organizationId);
        if (workflow.getStatus() == WorkflowStatus.PAUSED) {
            return toResponse(workflow, organizationId);
        }
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only an active workflow can be paused"
            );
        }
        workflow.setStatus(WorkflowStatus.PAUSED);
        workflow.setUpdatedByUser(userRepository.getReferenceById(actorUserId));
        WorkflowDefinition saved = definitionRepository.saveAndFlush(workflow);
        auditLifecycle(
                organizationId,
                actorUserId,
                saved,
                WorkflowAuditAction.WORKFLOW_PAUSED
        );
        return toResponse(saved, organizationId);
    }

    @Transactional
    public WorkflowResponse archiveWorkflow(Long id, Long actorUserId) {
        Long organizationId = organizationProvider.getOrganizationId();
        requireFeature(organizationId);
        WorkflowDefinition workflow = findForUpdate(id, organizationId);
        if (workflow.getStatus() == WorkflowStatus.ARCHIVED) {
            return toResponse(workflow, organizationId);
        }
        if (workflow.getStatus() == WorkflowStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Pause an active workflow before archiving it"
            );
        }
        workflow.setStatus(WorkflowStatus.ARCHIVED);
        workflow.setArchivedAt(timeProvider.now());
        workflow.setUpdatedByUser(userRepository.getReferenceById(actorUserId));
        WorkflowDefinition saved = definitionRepository.saveAndFlush(workflow);
        auditLifecycle(
                organizationId,
                actorUserId,
                saved,
                WorkflowAuditAction.WORKFLOW_ARCHIVED
        );
        return toResponse(saved, organizationId);
    }

    private WorkflowStructure saveStructure(
            WorkflowDefinition workflow,
            Long organizationId,
            WorkflowTriggerRequest triggerRequest,
            List<WorkflowActionRequest> actionRequests
    ) {
        WebhookEventType eventType = configurationValidator.parseEventType(
                triggerRequest.eventType()
        );
        WorkflowTrigger trigger = triggerRepository
                .findByOrganizationIdAndWorkflowId(
                        organizationId,
                        workflow.getId()
                )
                .orElseGet(WorkflowTrigger::new);
        if (trigger.getId() == null) {
            trigger.setOrganization(workflow.getOrganization());
            trigger.setWorkflow(workflow);
        }
        trigger.setTriggerType(WorkflowTriggerType.CRM_EVENT);
        trigger.setEventType(eventType);
        trigger.setConditionConfig(
                configurationValidator.validateAndSerializeCondition(
                        eventType,
                        triggerRequest.conditionConfig()
                )
        );
        trigger.setEnabled(defaultTrue(triggerRequest.enabled()));
        WorkflowTrigger savedTrigger = triggerRepository.saveAndFlush(trigger);

        List<WorkflowAction> actions = java.util.stream.IntStream
                .range(0, actionRequests.size())
                .mapToObj(index -> createAction(
                        workflow,
                        eventType,
                        actionRequests.get(index),
                        index + 1
                ))
                .toList();
        return new WorkflowStructure(
                savedTrigger,
                actionRepository.saveAllAndFlush(actions)
        );
    }

    private WorkflowAction createAction(
            WorkflowDefinition workflow,
            WebhookEventType eventType,
            WorkflowActionRequest request,
            int order
    ) {
        WorkflowAction action = new WorkflowAction();
        action.setPublicActionId(idGenerator.actionId());
        action.setOrganization(workflow.getOrganization());
        action.setWorkflow(workflow);
        action.setDefinitionVersion(workflow.getDefinitionVersion());
        action.setActionOrder((short) order);
        action.setName(request.name().trim());
        action.setActionType(request.actionType());
        action.setConfiguration(
                configurationValidator.validateAndSerializeAction(
                        eventType,
                        request.actionType(),
                        request.configuration()
                )
        );
        action.setEnabled(defaultTrue(request.enabled()));
        action.setTimeoutSeconds(defaultValue(
                request.timeoutSeconds(),
                DEFAULT_ACTION_TIMEOUT_SECONDS
        ));
        return action;
    }

    private void validateActivation(
            WorkflowTrigger trigger,
            List<WorkflowAction> actions
    ) {
        if (!trigger.isEnabled()) {
            throw new IllegalArgumentException(
                    "Workflow activation requires an enabled trigger"
            );
        }
        List<WorkflowAction> enabledActions = actions.stream()
                .filter(WorkflowAction::isEnabled)
                .toList();
        if (enabledActions.isEmpty() || enabledActions.size() > 10) {
            throw new IllegalArgumentException(
                    "Workflow activation requires between one and ten enabled actions"
            );
        }
        configurationValidator.rejectDirectLoop(
                trigger.getEventType(),
                enabledActions
        );
    }

    private void applyDefinition(
            WorkflowDefinition workflow,
            String name,
            String description,
            WorkflowFailurePolicy failurePolicy,
            Integer executionTimeoutSeconds,
            Integer maximumAttempts,
            Integer maximumExecutionsPerHour
    ) {
        workflow.setName(name);
        workflow.setDescription(normalizeOptional(description));
        workflow.setFailurePolicy(failurePolicy == null
                ? WorkflowFailurePolicy.STOP_ON_FAILURE
                : failurePolicy);
        workflow.setExecutionTimeoutSeconds(defaultValue(
                executionTimeoutSeconds,
                DEFAULT_EXECUTION_TIMEOUT_SECONDS
        ));
        workflow.setMaximumAttempts(defaultValue(
                maximumAttempts,
                DEFAULT_MAXIMUM_ATTEMPTS
        ));
        workflow.setMaximumExecutionsPerHour(defaultValue(
                maximumExecutionsPerHour,
                DEFAULT_MAXIMUM_EXECUTIONS_PER_HOUR
        ));
    }

    private WorkflowStructures loadStructures(
            Long organizationId,
            Collection<WorkflowDefinition> workflows
    ) {
        List<Long> workflowIds = workflows.stream()
                .map(WorkflowDefinition::getId)
                .toList();
        if (workflowIds.isEmpty()) {
            return new WorkflowStructures(Map.of(), Map.of());
        }
        Map<Long, WorkflowTrigger> triggers = triggerRepository
                .findByOrganizationIdAndWorkflowIdIn(
                        organizationId,
                        workflowIds
                )
                .stream()
                .collect(Collectors.toMap(
                        trigger -> trigger.getWorkflow().getId(),
                        Function.identity()
                ));
        Map<Long, List<WorkflowAction>> actions = actionRepository
                .findByOrganizationIdAndWorkflowIdInAndRetiredAtIsNullOrderByActionOrderAsc(
                        organizationId,
                        workflowIds
                )
                .stream()
                .collect(Collectors.groupingBy(
                        action -> action.getWorkflow().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return new WorkflowStructures(triggers, actions);
    }

    private WorkflowResponse toResponse(
            WorkflowDefinition workflow,
            Long organizationId
    ) {
        return mapper.toResponse(
                workflow,
                currentTrigger(workflow, organizationId),
                currentActions(workflow, organizationId)
        );
    }

    private WorkflowTrigger currentTrigger(
            WorkflowDefinition workflow,
            Long organizationId
    ) {
        return triggerRepository.findByOrganizationIdAndWorkflowId(
                organizationId,
                workflow.getId()
        ).orElseThrow(() -> new IllegalStateException(
                "Workflow trigger is missing"
        ));
    }

    private List<WorkflowAction> currentActions(
            WorkflowDefinition workflow,
            Long organizationId
    ) {
        return actionRepository
                .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                        organizationId,
                        workflow.getId(),
                        workflow.getDefinitionVersion()
                );
    }

    private WorkflowDefinition findForUpdate(Long id, Long organizationId) {
        return definitionRepository.findForUpdate(id, organizationId)
                .orElseThrow(this::notFound);
    }

    private void requireEditable(WorkflowDefinition workflow) {
        if (workflow.getStatus() != WorkflowStatus.DRAFT
                && workflow.getStatus() != WorkflowStatus.PAUSED) {
            throw new IllegalArgumentException(
                    "Only draft or paused workflows can be edited"
            );
        }
    }

    private void requireUniqueName(
            Long organizationId,
            String name,
            Long workflowId
    ) {
        boolean exists = workflowId == null
                ? definitionRepository.existsByOrganizationIdAndNameIgnoreCase(
                        organizationId,
                        name
                )
                : definitionRepository
                        .existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                                organizationId,
                                name,
                                workflowId
                        );
        if (exists) {
            throw new IllegalArgumentException(
                    "Workflow name already exists in this organization"
            );
        }
    }

    private void auditLifecycle(
            Long organizationId,
            Long actorUserId,
            WorkflowDefinition workflow,
            WorkflowAuditAction action
    ) {
        auditService.log(
                organizationId,
                actorUserId,
                action,
                workflow.getId(),
                auditService.details(
                        "publicWorkflowId", workflow.getPublicWorkflowId(),
                        "name", workflow.getName(),
                        "status", workflow.getStatus().name(),
                        "definitionVersion", workflow.getDefinitionVersion()
                )
        );
    }

    private void requireFeature(Long organizationId) {
        featureAccessService.requireFeature(
                organizationId,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        );
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int defaultValue(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Workflow not found");
    }

    private record WorkflowStructure(
            WorkflowTrigger trigger,
            List<WorkflowAction> actions
    ) {
    }

    private record WorkflowStructures(
            Map<Long, WorkflowTrigger> triggers,
            Map<Long, List<WorkflowAction>> actions
    ) {
    }
}
