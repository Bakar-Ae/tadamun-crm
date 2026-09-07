package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import com.crm.backend.webhook.WebhookEvent;
import com.crm.backend.webhook.WebhookEventRepository;
import com.crm.backend.webhook.WebhookEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class WorkflowPersistenceRepositoryTest {

    @Autowired
    private WorkflowDefinitionRepository definitionRepository;

    @Autowired
    private WorkflowTriggerRepository triggerRepository;

    @Autowired
    private WorkflowActionRepository actionRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private WorkflowActionExecutionRepository actionExecutionRepository;

    @Autowired
    private WorkflowActionAttemptRepository attemptRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldPersistTenantBoundWorkflowGraphAndHistory() {
        User actor = createActor();
        Organization organization = createOrganization(actor);
        LocalDateTime now = LocalDateTime.of(2026, 9, 7, 15, 0);

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setPublicWorkflowId("wf_phase87_repository_test");
        workflow.setOrganization(organization);
        workflow.setName("New customer follow-up");
        workflow.setCreatedByUser(actor);
        workflow = definitionRepository.saveAndFlush(workflow);

        WorkflowTrigger trigger = new WorkflowTrigger();
        trigger.setOrganization(organization);
        trigger.setWorkflow(workflow);
        trigger.setEventType(WebhookEventType.CUSTOMER_CREATED);
        trigger.setConditionConfig(
                "{\"field\":\"data.customer.status\","
                        + "\"operator\":\"EQUALS\",\"value\":\"ACTIVE\"}"
        );
        triggerRepository.saveAndFlush(trigger);

        WorkflowAction action = new WorkflowAction();
        action.setPublicActionId("wfa_phase87_repository_test");
        action.setOrganization(organization);
        action.setWorkflow(workflow);
        action.setDefinitionVersion(workflow.getDefinitionVersion());
        action.setActionOrder((short) 1);
        action.setName("Create follow-up task");
        action.setActionType(WorkflowActionType.CREATE_TASK);
        action.setConfiguration(
                "{\"titleTemplate\":\"Follow up {{customer.name}}\"}"
        );
        action = actionRepository.saveAndFlush(action);

        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId("evt_phase87_repository_test");
        event.setOrganization(organization);
        event.setEventType(WebhookEventType.CUSTOMER_CREATED);
        event.setAggregateType("CUSTOMER");
        event.setAggregateId(42L);
        event.setPayload("{\"data\":{\"customer\":{\"id\":42}}}");
        event.setNextPublicationAttemptAt(now);
        event.setOccurredAt(now);
        event = webhookEventRepository.saveAndFlush(event);

        WorkflowExecution execution = new WorkflowExecution();
        execution.setPublicExecutionId("wfx_phase87_repository_test");
        execution.setOrganization(organization);
        execution.setWorkflow(workflow);
        execution.setSourceEvent(event);
        execution.setWorkflowVersion(workflow.getDefinitionVersion());
        execution.setTriggerEventType(WebhookEventType.CUSTOMER_CREATED);
        execution.setSourceEventPublicId(event.getPublicEventId());
        execution.setCorrelationId("cor_phase87_repository_test");
        execution.setNextAttemptAt(now);
        execution.setInputPayload(event.getPayload());
        execution = executionRepository.saveAndFlush(execution);

        WorkflowActionExecution actionExecution =
                new WorkflowActionExecution();
        actionExecution.setPublicActionExecutionId(
                "wfax_phase87_repository_test"
        );
        actionExecution.setOrganization(organization);
        actionExecution.setExecution(execution);
        actionExecution.setAction(action);
        actionExecution.setActionOrder(action.getActionOrder());
        actionExecution.setActionType(action.getActionType());
        actionExecution.setConfigurationSnapshot(action.getConfiguration());
        actionExecution.setNextAttemptAt(now);
        actionExecution.setIdempotencyKey(
                "workflow-action:phase87:repository-test"
        );
        actionExecution = actionExecutionRepository.saveAndFlush(
                actionExecution
        );

        WorkflowActionAttempt attempt = new WorkflowActionAttempt();
        attempt.setOrganization(organization);
        attempt.setActionExecution(actionExecution);
        attempt.setAttemptNumber(1);
        attempt.setOutcome(WorkflowActionAttemptOutcome.SUCCEEDED);
        attempt.setDurationMs(25);
        attempt.setResultSummary("{\"resourceType\":\"TASK\"}");
        attemptRepository.saveAndFlush(attempt);

        Long organizationId = organization.getId();
        assertEquals(
                workflow.getId(),
                definitionRepository.findByIdAndOrganizationId(
                        workflow.getId(),
                        organizationId
                ).orElseThrow().getId()
        );
        assertEquals(
                WebhookEventType.CUSTOMER_CREATED,
                triggerRepository.findByOrganizationIdAndWorkflowId(
                        organizationId,
                        workflow.getId()
                ).orElseThrow().getEventType()
        );
        assertEquals(
                1,
                actionRepository
                        .findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
                                organizationId,
                                workflow.getId(),
                                1
                        )
                        .size()
        );
        assertEquals(
                1,
                actionExecutionRepository
                        .findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
                                organizationId,
                                execution.getId()
                        )
                        .size()
        );
        assertEquals(
                1,
                attemptRepository
                        .findByOrganizationIdAndActionExecutionIdOrderByAttemptNumberAsc(
                                organizationId,
                                actionExecution.getId()
                        )
                        .size()
        );
        assertTrue(definitionRepository.findByIdAndOrganizationId(
                workflow.getId(),
                organizationId + 1_000L
        ).isEmpty());
    }

    private User createActor() {
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User actor = new User();
        actor.setFullName("Phase 87 Workflow Administrator");
        actor.setEmail("phase87.workflow.admin@crm.test");
        actor.setPasswordHash("integration-test-password-hash");
        actor.setRole(role);
        actor.setStatus(UserStatus.ACTIVE);
        return userRepository.save(actor);
    }

    private Organization createOrganization(User actor) {
        Organization organization = new Organization();
        organization.setName("Phase 87 Organization");
        organization.setSlug("phase-87-organization");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(actor);
        return organizationRepository.save(organization);
    }
}
