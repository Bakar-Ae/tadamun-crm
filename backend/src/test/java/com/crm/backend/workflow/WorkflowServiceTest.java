package com.crm.backend.workflow;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.organization.Organization;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowServiceTest {

    private WorkflowDefinitionRepository definitionRepository;
    private WorkflowTriggerRepository triggerRepository;
    private WorkflowActionRepository actionRepository;
    private SubscriptionFeatureAccessService featureAccessService;
    private SubscriptionTimeProvider timeProvider;
    private WorkflowAuditService auditService;
    private WorkflowService service;
    private ObjectMapper objectMapper;
    private Organization organization;
    private User actor;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(WorkflowDefinitionRepository.class);
        triggerRepository = mock(WorkflowTriggerRepository.class);
        actionRepository = mock(WorkflowActionRepository.class);
        CurrentOrganizationProvider organizationProvider = mock(
                CurrentOrganizationProvider.class
        );
        UserRepository userRepository = mock(UserRepository.class);
        featureAccessService = mock(SubscriptionFeatureAccessService.class);
        timeProvider = mock(SubscriptionTimeProvider.class);
        auditService = mock(WorkflowAuditService.class);
        WorkflowPublicIdGenerator idGenerator = mock(
                WorkflowPublicIdGenerator.class
        );
        objectMapper = new ObjectMapper();

        organization = new Organization();
        organization.setId(42L);
        actor = new User();
        actor.setId(7L);
        actor.setFullName("Workflow Administrator");

        when(organizationProvider.getOrganizationId()).thenReturn(42L);
        when(organizationProvider.getOrganizationReference())
                .thenReturn(organization);
        when(userRepository.getReferenceById(7L)).thenReturn(actor);
        when(idGenerator.workflowId()).thenReturn("wf_fixed");
        when(idGenerator.actionId()).thenReturn("wfa_fixed");
        when(definitionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    WorkflowDefinition workflow = invocation.getArgument(0);
                    if (workflow.getId() == null) {
                        workflow.setId(99L);
                    }
                    return workflow;
                });
        when(triggerRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    WorkflowTrigger trigger = invocation.getArgument(0);
                    if (trigger.getId() == null) {
                        trigger.setId(101L);
                    }
                    return trigger;
                });
        when(actionRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> {
                    List<WorkflowAction> actions = invocation.getArgument(0);
                    for (int index = 0; index < actions.size(); index++) {
                        actions.get(index).setId(200L + index);
                    }
                    return actions;
                });
        when(auditService.details(any())).thenReturn(Map.of());
        when(timeProvider.now()).thenReturn(
                LocalDateTime.of(2026, 9, 7, 16, 0)
        );

        WorkflowConfigurationValidator validator =
                new WorkflowConfigurationValidator(objectMapper);
        service = new WorkflowService(
                definitionRepository,
                triggerRepository,
                actionRepository,
                validator,
                new WorkflowMapper(objectMapper),
                idGenerator,
                organizationProvider,
                userRepository,
                featureAccessService,
                timeProvider,
                auditService
        );
    }

    @Test
    void createShouldPersistSafeTenantScopedDraftWithDefaults()
            throws Exception {
        when(triggerRepository.findByOrganizationIdAndWorkflowId(42L, 99L))
                .thenReturn(Optional.empty());

        WorkflowResponse response = service.createWorkflow(
                createRequest(WebhookEventType.CUSTOMER_CREATED),
                7L
        );

        assertEquals(99L, response.id());
        assertEquals("wf_fixed", response.publicWorkflowId());
        assertEquals(WorkflowStatus.DRAFT, response.status());
        assertEquals(1, response.definitionVersion());
        assertEquals(60, response.executionTimeoutSeconds());
        assertEquals(3, response.maximumAttempts());
        assertEquals(100, response.maximumExecutionsPerHour());
        assertEquals(1, response.actions().size());
        assertEquals(42L, organization.getId());
        verify(featureAccessService).requireFeature(
                42L,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        );
    }

    @Test
    void updateShouldRetirePreviousActionsAndCreateNewRevision()
            throws Exception {
        WorkflowDefinition workflow = workflow(WorkflowStatus.PAUSED);
        WorkflowTrigger trigger = trigger(
                workflow,
                WebhookEventType.CUSTOMER_CREATED
        );
        WorkflowAction previous = action(
                workflow,
                WorkflowActionType.CREATE_TASK
        );
        when(definitionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(workflow));
        when(actionRepository
                .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                        42L,
                        99L,
                        1
                )).thenReturn(List.of(previous));
        when(triggerRepository.findByOrganizationIdAndWorkflowId(42L, 99L))
                .thenReturn(Optional.of(trigger));

        WorkflowResponse response = service.updateWorkflow(
                99L,
                updateRequest(),
                7L
        );

        assertNotNull(previous.getRetiredAt());
        assertEquals(2, workflow.getDefinitionVersion());
        assertEquals(2, response.definitionVersion());
        assertEquals(
                WorkflowActionType.SEND_IN_APP_NOTIFICATION,
                response.actions().getFirst().actionType()
        );
        verify(actionRepository).saveAll(List.of(previous));
    }

    @Test
    void activateShouldEnforceCapacityAndMarkWorkflowActive() {
        WorkflowDefinition workflow = workflow(WorkflowStatus.DRAFT);
        WorkflowTrigger trigger = trigger(
                workflow,
                WebhookEventType.CUSTOMER_CREATED
        );
        WorkflowAction action = action(
                workflow,
                WorkflowActionType.CREATE_TASK
        );
        when(featureAccessService.requireFeatureAndGetLimitForUpdate(
                42L,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        )).thenReturn(10L);
        when(definitionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(workflow));
        when(definitionRepository.countByOrganizationIdAndStatus(
                42L,
                WorkflowStatus.ACTIVE
        )).thenReturn(2L);
        when(triggerRepository.findByOrganizationIdAndWorkflowId(42L, 99L))
                .thenReturn(Optional.of(trigger));
        when(actionRepository
                .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                        42L,
                        99L,
                        1
                )).thenReturn(List.of(action));

        WorkflowResponse response = service.activateWorkflow(99L, 7L);

        assertEquals(WorkflowStatus.ACTIVE, response.status());
        assertEquals(
                LocalDateTime.of(2026, 9, 7, 16, 0),
                response.activatedAt()
        );
    }

    @Test
    void activateShouldStopWhenPlanLimitIsReached() {
        WorkflowDefinition workflow = workflow(WorkflowStatus.DRAFT);
        when(featureAccessService.requireFeatureAndGetLimitForUpdate(
                42L,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        )).thenReturn(10L);
        when(definitionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(workflow));
        when(definitionRepository.countByOrganizationIdAndStatus(
                42L,
                WorkflowStatus.ACTIVE
        )).thenReturn(10L);

        assertThrows(
                SubscriptionLimitExceededException.class,
                () -> service.activateWorkflow(99L, 7L)
        );
        verify(triggerRepository, never())
                .findByOrganizationIdAndWorkflowId(42L, 99L);
    }

    @Test
    void activateShouldRejectDirectTaskCreationLoop() {
        WorkflowDefinition workflow = workflow(WorkflowStatus.DRAFT);
        WorkflowTrigger trigger = trigger(
                workflow,
                WebhookEventType.TASK_CREATED
        );
        WorkflowAction action = action(
                workflow,
                WorkflowActionType.CREATE_TASK
        );
        when(featureAccessService.requireFeatureAndGetLimitForUpdate(
                42L,
                SubscriptionFeature.WORKFLOW_AUTOMATION
        )).thenReturn(null);
        when(definitionRepository.findForUpdate(99L, 42L))
                .thenReturn(Optional.of(workflow));
        when(triggerRepository.findByOrganizationIdAndWorkflowId(42L, 99L))
                .thenReturn(Optional.of(trigger));
        when(actionRepository
                .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                        42L,
                        99L,
                        1
                )).thenReturn(List.of(action));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.activateWorkflow(99L, 7L)
        );
    }

    @Test
    void getShouldHideWorkflowFromAnotherOrganization() {
        when(definitionRepository.findByIdAndOrganizationId(99L, 42L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getWorkflow(99L)
        );
        verify(definitionRepository).findByIdAndOrganizationId(99L, 42L);
    }

    private CreateWorkflowRequest createRequest(WebhookEventType eventType)
            throws Exception {
        return new CreateWorkflowRequest(
                " Customer follow-up ",
                " Create a task for new customers ",
                null,
                null,
                null,
                null,
                new WorkflowTriggerRequest(
                        eventType.getValue(),
                        null,
                        null
                ),
                List.of(new WorkflowActionRequest(
                        " Follow up ",
                        WorkflowActionType.CREATE_TASK,
                        json("""
                                {
                                  "titleTemplate": "Follow up {{customer.name}}",
                                  "priority": "HIGH"
                                }
                                """),
                        null,
                        null
                ))
        );
    }

    private UpdateWorkflowRequest updateRequest() throws Exception {
        return new UpdateWorkflowRequest(
                "Customer welcome",
                "Notify the workflow creator",
                WorkflowFailurePolicy.CONTINUE_ON_FAILURE,
                90,
                4,
                200,
                new WorkflowTriggerRequest(
                        WebhookEventType.CUSTOMER_CREATED.getValue(),
                        null,
                        true
                ),
                List.of(new WorkflowActionRequest(
                        "Notify creator",
                        WorkflowActionType.SEND_IN_APP_NOTIFICATION,
                        json("""
                                {
                                  "titleTemplate": "New customer",
                                  "messageTemplate": "Welcome {{customer.name}}",
                                  "recipientPolicy": "WORKFLOW_CREATOR"
                                }
                                """),
                        true,
                        10
                ))
        );
    }

    private WorkflowDefinition workflow(WorkflowStatus status) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(99L);
        workflow.setPublicWorkflowId("wf_fixed");
        workflow.setOrganization(organization);
        workflow.setName("Customer follow-up");
        workflow.setStatus(status);
        workflow.setCreatedByUser(actor);
        return workflow;
    }

    private WorkflowTrigger trigger(
            WorkflowDefinition workflow,
            WebhookEventType eventType
    ) {
        WorkflowTrigger trigger = new WorkflowTrigger();
        trigger.setId(101L);
        trigger.setOrganization(organization);
        trigger.setWorkflow(workflow);
        trigger.setEventType(eventType);
        return trigger;
    }

    private WorkflowAction action(
            WorkflowDefinition workflow,
            WorkflowActionType actionType
    ) {
        WorkflowAction action = new WorkflowAction();
        action.setId(201L);
        action.setPublicActionId("wfa_existing");
        action.setOrganization(organization);
        action.setWorkflow(workflow);
        action.setDefinitionVersion(1);
        action.setActionOrder((short) 1);
        action.setName("Existing action");
        action.setActionType(actionType);
        action.setConfiguration("{\"titleTemplate\":\"Follow up\"}");
        return action;
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
