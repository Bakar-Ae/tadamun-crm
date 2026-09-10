package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowActionResultServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026,
            9,
            8,
            16,
            0
    );

    @Mock
    private WorkflowActionExecutionRepository actionRepository;

    @Mock
    private WorkflowActionAttemptRepository attemptRepository;

    @Mock
    private WorkflowActionExecutor actionExecutor;

    @Mock
    private WorkflowAuditService auditService;

    @Mock
    private SubscriptionTimeProvider timeProvider;

    private WorkflowActionResultService service;

    @BeforeEach
    void setUp() {
        WorkflowWorkerProperties properties = new WorkflowWorkerProperties();
        when(timeProvider.now()).thenReturn(NOW);
        service = new WorkflowActionResultService(
                actionRepository,
                attemptRepository,
                actionExecutor,
                auditService,
                timeProvider,
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void shouldCompleteActionAndReleaseNextAction() {
        WorkflowActionExecution current = claimedAction(1, 1);
        WorkflowActionExecution next = pendingAction(
                current.getExecution(),
                2
        );
        when(actionRepository.findForUpdate(10L))
                .thenReturn(Optional.of(current));
        when(actionExecutor.execute(current)).thenReturn(
                new WorkflowActionResult("TASK", 42L, Map.of("ok", true))
        );
        when(actionRepository
                .findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
                        1L,
                        20L
                ))
                .thenReturn(List.of(current, next));

        service.processClaim(new WorkflowQueueClaim(10L, "claim-token"));

        assertEquals(
                WorkflowActionExecutionStatus.SUCCEEDED,
                current.getStatus()
        );
        assertEquals(NOW, next.getNextAttemptAt());
        assertEquals(WorkflowExecutionStatus.PENDING,
                current.getExecution().getStatus());
        verify(attemptRepository).save(
                org.mockito.ArgumentMatchers.any(WorkflowActionAttempt.class)
        );
    }

    @Test
    void shouldScheduleRetryForTransientFailure() {
        WorkflowActionExecution current = claimedAction(1, 1);
        when(actionRepository.findForUpdate(10L))
                .thenReturn(Optional.of(current));
        when(actionExecutor.execute(current)).thenThrow(
                new IllegalStateException("Temporary database failure")
        );

        service.processClaim(new WorkflowQueueClaim(10L, "claim-token"));

        assertEquals(
                WorkflowActionExecutionStatus.RETRY_SCHEDULED,
                current.getStatus()
        );
        assertEquals(
                WorkflowExecutionStatus.RETRY_SCHEDULED,
                current.getExecution().getStatus()
        );
        assertEquals(NOW.plusMinutes(1), current.getNextAttemptAt());
        assertNotNull(current.getLastError());
    }

    @Test
    void shouldStartFreshRetryCycleWithoutReusingAttemptNumbers() {
        WorkflowActionExecution current = claimedAction(1, 4);
        when(actionRepository.findForUpdate(10L))
                .thenReturn(Optional.of(current));
        when(actionExecutor.execute(current)).thenThrow(
                new IllegalStateException("Temporary database failure")
        );

        service.processClaim(new WorkflowQueueClaim(10L, "claim-token"));

        ArgumentCaptor<WorkflowActionAttempt> attemptCaptor =
                ArgumentCaptor.forClass(WorkflowActionAttempt.class);
        verify(attemptRepository).save(attemptCaptor.capture());
        assertEquals(4, attemptCaptor.getValue().getAttemptNumber());
        assertEquals(
                WorkflowActionExecutionStatus.RETRY_SCHEDULED,
                current.getStatus()
        );
        assertEquals(NOW.plusMinutes(1), current.getNextAttemptAt());
    }

    @Test
    void shouldStopAndSkipRemainingActionsForTerminalFailure() {
        WorkflowActionExecution current = claimedAction(1, 1);
        WorkflowActionExecution next = pendingAction(
                current.getExecution(),
                2
        );
        when(actionRepository.findForUpdate(10L))
                .thenReturn(Optional.of(current));
        when(actionExecutor.execute(current)).thenThrow(
                new IllegalArgumentException("Invalid workflow relation")
        );
        when(actionRepository
                .findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
                        1L,
                        20L
                ))
                .thenReturn(List.of(current, next));

        service.processClaim(new WorkflowQueueClaim(10L, "claim-token"));

        assertEquals(WorkflowActionExecutionStatus.FAILED, current.getStatus());
        assertEquals(WorkflowActionExecutionStatus.SKIPPED, next.getStatus());
        assertEquals(
                WorkflowExecutionStatus.FAILED,
                current.getExecution().getStatus()
        );
        assertNull(current.getExecution().getCurrentActionOrder());
    }

    private WorkflowActionExecution claimedAction(int order, int attempts) {
        WorkflowActionExecution action = pendingAction(execution(), order);
        action.setId(10L);
        action.setStatus(WorkflowActionExecutionStatus.PROCESSING);
        action.setAttemptCount(attempts);
        action.setClaimToken("claim-token");
        action.setClaimedAt(NOW.minusSeconds(5));
        return action;
    }

    private WorkflowActionExecution pendingAction(
            WorkflowExecution execution,
            int order
    ) {
        WorkflowActionExecution action = new WorkflowActionExecution();
        action.setOrganization(execution.getOrganization());
        action.setExecution(execution);
        action.setActionOrder((short) order);
        action.setStatus(WorkflowActionExecutionStatus.PENDING);
        return action;
    }

    private WorkflowExecution execution() {
        Organization organization = new Organization();
        organization.setId(1L);
        User creator = new User();
        creator.setId(7L);
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(5L);
        workflow.setFailurePolicy(WorkflowFailurePolicy.STOP_ON_FAILURE);
        workflow.setMaximumAttempts(3);
        workflow.setCreatedByUser(creator);
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(20L);
        execution.setPublicExecutionId("wfx_test");
        execution.setOrganization(organization);
        execution.setWorkflow(workflow);
        execution.setCurrentActionOrder((short) 1);
        return execution;
    }
}
