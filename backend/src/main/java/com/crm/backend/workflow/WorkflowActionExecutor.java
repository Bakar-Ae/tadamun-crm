package com.crm.backend.workflow;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.Lead;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.notification.NotificationService;
import com.crm.backend.notification.NotificationType;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.task.CrmTask;
import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskStatus;
import com.crm.backend.user.User;
import com.crm.backend.user.UserStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WorkflowActionExecutor {

    private static final Pattern TEMPLATE_TOKEN = Pattern.compile(
            "\\{\\{([a-zA-Z][a-zA-Z0-9.]*)}}"
    );

    private final TaskRepository taskRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final SubscriptionTimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    public WorkflowActionExecutor(
            TaskRepository taskRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            OrganizationMembershipRepository membershipRepository,
            NotificationService notificationService,
            AuditLogService auditLogService,
            SubscriptionTimeProvider timeProvider,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    public WorkflowActionResult execute(
            WorkflowActionExecution actionExecution
    ) {
        JsonNode configuration = readJson(
                actionExecution.getConfigurationSnapshot(),
                "Workflow action configuration is invalid"
        );
        JsonNode payload = readJson(
                actionExecution.getExecution().getInputPayload(),
                "Workflow event payload is invalid"
        );
        return switch (actionExecution.getActionType()) {
            case CREATE_TASK -> createTask(
                    actionExecution,
                    configuration,
                    payload
            );
            case SEND_IN_APP_NOTIFICATION -> sendNotification(
                    actionExecution,
                    configuration,
                    payload
            );
        };
    }

    private WorkflowActionResult createTask(
            WorkflowActionExecution actionExecution,
            JsonNode configuration,
            JsonNode payload
    ) {
        WorkflowExecution execution = actionExecution.getExecution();
        Long organizationId = execution.getOrganization().getId();
        User assignee = resolveUser(
                actionExecution,
                payload,
                text(configuration, "assigneePolicy", "RECORD_OWNER_OR_ASSIGNEE"),
                true
        );

        CrmTask task = new CrmTask();
        task.setOrganization(execution.getOrganization());
        String title = render(
                configuration.get("titleTemplate").asText(),
                payload
        );
        if (title.isBlank()) {
            throw new IllegalArgumentException(
                    "Workflow task title resolved to an empty value"
            );
        }
        task.setTitle(title);
        task.setPriority(TaskPriority.valueOf(
                text(configuration, "priority", TaskPriority.MEDIUM.name())
        ));
        task.setStatus(TaskStatus.OPEN);
        int dueDateOffsetDays = integer(
                configuration,
                "dueDateOffsetDays",
                0
        );
        task.setDueDate(timeProvider.now().plusDays(dueDateOffsetDays));
        task.setAssignedToUser(assignee);

        if ("TRIGGER_RECORD".equals(text(
                configuration,
                "relationPolicy",
                "TRIGGER_RECORD"
        ))) {
            task.setCustomer(resolveCustomer(execution, payload, organizationId));
            task.setLead(resolveLead(execution, payload, organizationId));
        }

        CrmTask savedTask = taskRepository.save(task);
        auditLogService.logForOrganization(
                execution.getOrganization(),
                execution.getWorkflow().getCreatedByUser().getId(),
                "WORKFLOW_TASK_CREATED",
                "TASK",
                savedTask.getId(),
                "{\"workflowExecutionId\":\""
                        + execution.getPublicExecutionId() + "\"}"
        );
        return new WorkflowActionResult(
                "TASK",
                savedTask.getId(),
                Map.of("title", savedTask.getTitle())
        );
    }

    private WorkflowActionResult sendNotification(
            WorkflowActionExecution actionExecution,
            JsonNode configuration,
            JsonNode payload
    ) {
        WorkflowExecution execution = actionExecution.getExecution();
        User recipient = resolveUser(
                actionExecution,
                payload,
                text(configuration, "recipientPolicy", "RECORD_OWNER_OR_ASSIGNEE"),
                false
        );
        boolean created = notificationService.createNotificationOnce(
                execution.getOrganization(),
                recipient.getId(),
                render(configuration.get("titleTemplate").asText(), payload),
                render(configuration.get("messageTemplate").asText(), payload),
                NotificationType.SYSTEM,
                "workflow:" + actionExecution.getIdempotencyKey()
        );
        return new WorkflowActionResult(
                "NOTIFICATION",
                null,
                Map.of(
                        "recipientUserId", recipient.getId(),
                        "created", created
                )
        );
    }

    private User resolveUser(
            WorkflowActionExecution actionExecution,
            JsonNode payload,
            String policy,
            boolean nullable
    ) {
        if ("UNASSIGNED".equals(policy) && nullable) {
            return null;
        }

        Long userId = "WORKFLOW_CREATOR".equals(policy)
                ? actionExecution.getExecution().getWorkflow()
                        .getCreatedByUser().getId()
                : recordOwnerOrAssignee(payload);
        if (userId == null) {
            if (nullable) {
                return null;
            }
            throw new IllegalArgumentException(
                    "Workflow notification recipient could not be resolved"
            );
        }

        Long organizationId = actionExecution.getOrganization().getId();
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        organizationId,
                        userId,
                        OrganizationMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow user is not an active organization member"
                ));
        User user = membership.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Workflow user is inactive");
        }
        return user;
    }

    private Long recordOwnerOrAssignee(JsonNode payload) {
        JsonNode data = payload.path("data");
        Long value = firstLong(
                data.path("customer").get("ownerUserId"),
                data.path("lead").get("assignedToUserId"),
                data.path("task").get("assignedToUserId"),
                data.path("note").get("createdByUserId")
        );
        return value;
    }

    private Customer resolveCustomer(
            WorkflowExecution execution,
            JsonNode payload,
            Long organizationId
    ) {
        Long customerId = "CUSTOMER".equals(execution.getSourceEvent().getAggregateType())
                ? execution.getSourceEvent().getAggregateId()
                : firstLong(
                        payload.path("data").path("customer").get("id"),
                        payload.path("data").path("contact").get("customerId"),
                        payload.path("data").path("task").get("customerId"),
                        payload.path("data").path("note").get("customerId")
                );
        if (customerId == null) {
            return null;
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow customer relation was not found"
                ));
        if (!organizationId.equals(customer.getOrganization().getId())) {
            throw new IllegalArgumentException(
                    "Workflow customer relation belongs to another organization"
            );
        }
        return customer;
    }

    private Lead resolveLead(
            WorkflowExecution execution,
            JsonNode payload,
            Long organizationId
    ) {
        Long leadId = "LEAD".equals(execution.getSourceEvent().getAggregateType())
                ? execution.getSourceEvent().getAggregateId()
                : firstLong(
                        payload.path("data").path("lead").get("id"),
                        payload.path("data").path("task").get("leadId"),
                        payload.path("data").path("note").get("leadId")
                );
        if (leadId == null) {
            return null;
        }
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow lead relation was not found"
                ));
        if (!organizationId.equals(lead.getOrganization().getId())) {
            throw new IllegalArgumentException(
                    "Workflow lead relation belongs to another organization"
            );
        }
        return lead;
    }

    private String render(String template, JsonNode payload) {
        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            JsonNode value = path(payload.path("data"), matcher.group(1));
            matcher.appendReplacement(
                    rendered,
                    Matcher.quoteReplacement(
                            value == null || value.isNull()
                                    ? ""
                                    : value.asText()
                    )
            );
        }
        matcher.appendTail(rendered);
        return rendered.toString().trim();
    }

    private JsonNode path(JsonNode root, String dottedPath) {
        JsonNode current = root;
        for (String segment : dottedPath.split("\\.")) {
            current = current == null ? null : current.get(segment);
        }
        return current;
    }

    private Long firstLong(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && candidate.canConvertToLong()) {
                return candidate.asLong();
            }
        }
        return null;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return value == null ? defaultValue : value.asText();
    }

    private int integer(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        return value == null ? defaultValue : value.asInt();
    }

    private JsonNode readJson(String value, String errorMessage) {
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }
}
