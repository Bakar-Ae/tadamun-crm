package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.webhook.WebhookEvent;
import com.crm.backend.webhook.WebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowEventDispatchServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 9, 8, 12, 0);

    private WorkflowTriggerRepository triggerRepository;
    private WorkflowDefinitionRepository definitionRepository;
    private WorkflowActionRepository actionRepository;
    private WorkflowExecutionRepository executionRepository;
    private WorkflowActionExecutionRepository actionExecutionRepository;
    private WorkflowConditionMatcher conditionMatcher;
    private WorkflowPublicIdGenerator idGenerator;
    private WorkflowEventDispatchService service;
    private WorkflowDefinition workflow;
    private WorkflowTrigger trigger;
    private WebhookEvent sourceEvent;
    private List<WorkflowAction> actions;

    @BeforeEach
    void setUp() {
        triggerRepository = mock(WorkflowTriggerRepository.class);
        definitionRepository = mock(WorkflowDefinitionRepository.class);
        actionRepository = mock(WorkflowActionRepository.class);
        executionRepository = mock(WorkflowExecutionRepository.class);
        actionExecutionRepository = mock(
                WorkflowActionExecutionRepository.class
        );
        conditionMatcher = mock(WorkflowConditionMatcher.class);
        idGenerator = mock(WorkflowPublicIdGenerator.class);
        SubscriptionTimeProvider timeProvider = mock(
                SubscriptionTimeProvider.class
        );

        service = new WorkflowEventDispatchService(
                triggerRepository,
                definitionRepository,
                actionRepository,
                executionRepository,
                actionExecutionRepository,
                conditionMatcher,
                idGenerator,
                timeProvider
        );
        when(timeProvider.now()).thenReturn(NOW);
        when(idGenerator.executionId()).thenReturn("wfx_test");
        when(idGenerator.actionExecutionId())
                .thenReturn("wfax_first", "wfax_second");
        when(executionRepository.save(any(WorkflowExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        createFixture();
    }

    @Test
    void shouldCreateExecutionAndOrderedActionSnapshots() {
        stubMatchingWorkflow();
        when(actionRepository
                .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                        42L,
                        10L,
                        3L
                )).thenReturn(actions);

        int created = service.dispatch(sourceEvent);

        assertEquals(1, created);
        ArgumentCaptor<WorkflowExecution> executionCaptor =
                ArgumentCaptor.forClass(WorkflowExecution.class);
        verify(executionRepository).save(executionCaptor.capture());
        WorkflowExecution execution = executionCaptor.getValue();
        assertEquals(WorkflowExecutionStatus.PENDING, execution.getStatus());
        assertEquals(3L, execution.getWorkflowVersion());
        assertEquals("evt_source", execution.getCorrelationId());
        assertEquals((short) 1, execution.getCurrentActionOrder());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkflowActionExecution>> actionCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(actionExecutionRepository).saveAll(actionCaptor.capture());
        List<WorkflowActionExecution> snapshots = actionCaptor.getValue();
        assertEquals(2, snapshots.size());
        assertEquals(NOW, snapshots.get(0).getNextAttemptAt());
        assertNull(snapshots.get(1).getNextAttemptAt());
        assertEquals(
                "wfx_test:wfa_first",
                snapshots.get(0).getIdempotencyKey()
        );
    }

    @Test
    void shouldSkipConditionMismatch() {
        when(triggerRepository.findMatchingTriggers(
                42L,
                WebhookEventType.CUSTOMER_CREATED,
                WorkflowStatus.ACTIVE
        )).thenReturn(List.of(trigger));
        when(conditionMatcher.matches(
                WebhookEventType.CUSTOMER_CREATED,
                trigger.getConditionConfig(),
                sourceEvent.getPayload()
        )).thenReturn(false);

        assertEquals(0, service.dispatch(sourceEvent));

        verify(definitionRepository, never()).findForUpdate(any(), any());
        verify(executionRepository, never()).save(any());
    }

    @Test
    void shouldSkipDuplicateExecution() {
        stubMatchingWorkflow();
        when(executionRepository
                .existsByOrganizationIdAndWorkflowIdAndSourceEventId(
                        42L,
                        10L,
                        99L
                )).thenReturn(true);

        assertEquals(0, service.dispatch(sourceEvent));

        verify(actionRepository, never())
                .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                        any(),
                        any(),
                        any(Long.class)
                );
        verify(executionRepository, never()).save(any());
    }

    @Test
    void shouldEnforceHourlyExecutionLimit() {
        stubMatchingWorkflow();
        when(executionRepository
                .countByOrganizationIdAndWorkflowIdAndCreatedAtGreaterThanEqual(
                        42L,
                        10L,
                        NOW.minusHours(1)
                )).thenReturn(2L);

        assertEquals(0, service.dispatch(sourceEvent));

        verify(executionRepository, never()).save(any());
    }

    private void stubMatchingWorkflow() {
        when(triggerRepository.findMatchingTriggers(
                42L,
                WebhookEventType.CUSTOMER_CREATED,
                WorkflowStatus.ACTIVE
        )).thenReturn(List.of(trigger));
        when(conditionMatcher.matches(
                WebhookEventType.CUSTOMER_CREATED,
                trigger.getConditionConfig(),
                sourceEvent.getPayload()
        )).thenReturn(true);
        when(definitionRepository.findForUpdate(10L, 42L))
                .thenReturn(Optional.of(workflow));
    }

    private void createFixture() {
        Organization organization = new Organization();
        organization.setId(42L);

        workflow = new WorkflowDefinition();
        workflow.setId(10L);
        workflow.setOrganization(organization);
        workflow.setStatus(WorkflowStatus.ACTIVE);
        workflow.setDefinitionVersion(3L);
        workflow.setMaximumExecutionsPerHour(2);

        trigger = new WorkflowTrigger();
        trigger.setId(20L);
        trigger.setOrganization(organization);
        trigger.setWorkflow(workflow);
        trigger.setEventType(WebhookEventType.CUSTOMER_CREATED);
        trigger.setConditionConfig("""
                {
                  "field": "data.customer.status",
                  "operator": "EQUALS",
                  "value": "ACTIVE"
                }
                """);
        trigger.setEnabled(true);

        sourceEvent = new WebhookEvent();
        sourceEvent.setId(99L);
        sourceEvent.setPublicEventId("evt_source");
        sourceEvent.setOrganization(organization);
        sourceEvent.setEventType(WebhookEventType.CUSTOMER_CREATED);
        sourceEvent.setPayload("""
                {
                  "data": {
                    "customer": {
                      "id": 7,
                      "status": "ACTIVE"
                    }
                  }
                }
                """);

        actions = List.of(
                action(
                        100L,
                        "wfa_first",
                        (short) 1,
                        WorkflowActionType.CREATE_TASK
                ),
                action(
                        101L,
                        "wfa_second",
                        (short) 2,
                        WorkflowActionType.SEND_IN_APP_NOTIFICATION
                )
        );
    }

    private WorkflowAction action(
            Long id,
            String publicId,
            short order,
            WorkflowActionType type
    ) {
        WorkflowAction action = new WorkflowAction();
        action.setId(id);
        action.setPublicActionId(publicId);
        action.setOrganization(workflow.getOrganization());
        action.setWorkflow(workflow);
        action.setDefinitionVersion(workflow.getDefinitionVersion());
        action.setActionOrder(order);
        action.setName("Action " + order);
        action.setActionType(type);
        action.setConfiguration("{}");
        action.setEnabled(true);
        return action;
    }
}
