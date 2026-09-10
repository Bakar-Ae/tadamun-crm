package com.crm.backend.workflow;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.notification.NotificationService;
import com.crm.backend.notification.NotificationType;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.task.CrmTask;
import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.user.User;
import com.crm.backend.user.UserStatus;
import com.crm.backend.webhook.WebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowActionExecutorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026,
            9,
            8,
            16,
            30
    );

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SubscriptionTimeProvider timeProvider;

    private WorkflowActionExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new WorkflowActionExecutor(
                taskRepository,
                customerRepository,
                leadRepository,
                membershipRepository,
                notificationService,
                auditLogService,
                timeProvider,
                new ObjectMapper()
        );
    }

    @Test
    void shouldRenderAndCreateTenantBoundTask() {
        WorkflowActionExecution actionExecution = actionExecution(
                WorkflowActionType.CREATE_TASK,
                "{\"titleTemplate\":\"Follow up {{customer.name}}\"," 
                        + "\"priority\":\"HIGH\",\"dueDateOffsetDays\":2,"
                        + "\"assigneePolicy\":\"WORKFLOW_CREATOR\","
                        + "\"relationPolicy\":\"TRIGGER_RECORD\"}"
        );
        User creator = actionExecution.getExecution()
                .getWorkflow().getCreatedByUser();
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(creator);
        when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                1L,
                creator.getId(),
                OrganizationMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        Customer customer = new Customer();
        customer.setId(42L);
        customer.setOrganization(actionExecution.getOrganization());
        when(customerRepository.findById(42L))
                .thenReturn(Optional.of(customer));
        when(timeProvider.now()).thenReturn(NOW);
        when(taskRepository.save(any(CrmTask.class))).thenAnswer(invocation -> {
            CrmTask task = invocation.getArgument(0);
            task.setId(99L);
            return task;
        });

        WorkflowActionResult result = executor.execute(actionExecution);

        ArgumentCaptor<CrmTask> taskCaptor = ArgumentCaptor.forClass(
                CrmTask.class
        );
        verify(taskRepository).save(taskCaptor.capture());
        CrmTask task = taskCaptor.getValue();
        assertEquals("Follow up Acme", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(NOW.plusDays(2), task.getDueDate());
        assertEquals(creator, task.getAssignedToUser());
        assertEquals(customer, task.getCustomer());
        assertEquals(99L, result.resourceId());
    }

    @Test
    void shouldSendNotificationOnceWithActionIdempotencyKey() {
        WorkflowActionExecution actionExecution = actionExecution(
                WorkflowActionType.SEND_IN_APP_NOTIFICATION,
                "{\"titleTemplate\":\"Customer {{customer.name}}\"," 
                        + "\"messageTemplate\":\"Review {{customer.id}}\","
                        + "\"recipientPolicy\":\"WORKFLOW_CREATOR\"}"
        );
        User creator = actionExecution.getExecution()
                .getWorkflow().getCreatedByUser();
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(creator);
        when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                1L,
                creator.getId(),
                OrganizationMembershipStatus.ACTIVE
        )).thenReturn(Optional.of(membership));
        when(notificationService.createNotificationOnce(
                actionExecution.getOrganization(),
                creator.getId(),
                "Customer Acme",
                "Review 42",
                NotificationType.SYSTEM,
                "workflow:wfx_test:wfa_test"
        )).thenReturn(true);

        WorkflowActionResult result = executor.execute(actionExecution);

        assertEquals("NOTIFICATION", result.resourceType());
        assertEquals(true, result.summary().get("created"));
        verify(notificationService).createNotificationOnce(
                actionExecution.getOrganization(),
                creator.getId(),
                "Customer Acme",
                "Review 42",
                NotificationType.SYSTEM,
                "workflow:wfx_test:wfa_test"
        );
    }

    private WorkflowActionExecution actionExecution(
            WorkflowActionType actionType,
            String configuration
    ) {
        Organization organization = new Organization();
        organization.setId(1L);
        User creator = new User();
        creator.setId(7L);
        creator.setStatus(UserStatus.ACTIVE);
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(5L);
        workflow.setCreatedByUser(creator);
        WebhookEvent event = new WebhookEvent();
        event.setAggregateType("CUSTOMER");
        event.setAggregateId(42L);
        WorkflowExecution execution = new WorkflowExecution();
        execution.setPublicExecutionId("wfx_test");
        execution.setOrganization(organization);
        execution.setWorkflow(workflow);
        execution.setSourceEvent(event);
        execution.setInputPayload(
                "{\"data\":{\"customer\":{\"id\":42,"
                        + "\"name\":\"Acme\",\"ownerUserId\":7}}}"
        );
        WorkflowActionExecution actionExecution =
                new WorkflowActionExecution();
        actionExecution.setOrganization(organization);
        actionExecution.setExecution(execution);
        actionExecution.setActionType(actionType);
        actionExecution.setConfigurationSnapshot(configuration);
        actionExecution.setIdempotencyKey("wfx_test:wfa_test");
        return actionExecution;
    }
}
