package com.crm.backend.workflow;

import com.crm.backend.task.TaskPriority;
import com.crm.backend.webhook.WebhookEventType;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkflowConfigurationValidator {

    private static final int MAX_JSON_BYTES = 16 * 1024;
    private static final int MAX_CONDITION_DEPTH = 5;
    private static final int MAX_GROUP_CONDITIONS = 10;
    private static final int MAX_IN_VALUES = 20;
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile(
            "\\{\\{([a-zA-Z][a-zA-Z0-9.]*)}}"
    );
    private static final Set<String> CONDITION_OPERATORS = Set.of(
            "EQUALS",
            "NOT_EQUALS",
            "IN",
            "EXISTS"
    );
    private static final Set<String> TASK_CONFIGURATION_FIELDS = Set.of(
            "titleTemplate",
            "priority",
            "dueDateOffsetDays",
            "assigneePolicy",
            "relationPolicy"
    );
    private static final Set<String> NOTIFICATION_CONFIGURATION_FIELDS = Set.of(
            "titleTemplate",
            "messageTemplate",
            "recipientPolicy"
    );
    private static final Set<String> ASSIGNEE_POLICIES = Set.of(
            "RECORD_OWNER_OR_ASSIGNEE",
            "WORKFLOW_CREATOR",
            "UNASSIGNED"
    );
    private static final Set<String> RELATION_POLICIES = Set.of(
            "TRIGGER_RECORD",
            "NONE"
    );
    private static final Set<String> RECIPIENT_POLICIES = Set.of(
            "RECORD_OWNER_OR_ASSIGNEE",
            "WORKFLOW_CREATOR"
    );
    private static final Map<WebhookEventType, Set<String>> EVENT_FIELDS =
            eventFields();

    private final ObjectMapper objectMapper;

    public WorkflowConfigurationValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WebhookEventType parseEventType(String value) {
        return WebhookEventType.fromValue(value);
    }

    public String validateAndSerializeCondition(
            WebhookEventType eventType,
            JsonNode condition
    ) {
        if (condition == null || condition.isNull()) {
            return null;
        }
        String serialized = serializeBounded(condition, "Workflow condition");
        validateCondition(condition, eventType, 1);
        return serialized;
    }

    public String validateAndSerializeAction(
            WebhookEventType eventType,
            WorkflowActionType actionType,
            JsonNode configuration
    ) {
        requireObject(configuration, "Action configuration");
        String serialized = serializeBounded(
                configuration,
                "Action configuration"
        );
        switch (actionType) {
            case CREATE_TASK -> validateCreateTask(configuration, eventType);
            case SEND_IN_APP_NOTIFICATION ->
                    validateNotification(configuration, eventType);
        }
        return serialized;
    }

    public void rejectDirectLoop(
            WebhookEventType eventType,
            Iterable<WorkflowAction> actions
    ) {
        if (eventType != WebhookEventType.TASK_CREATED) {
            return;
        }
        for (WorkflowAction action : actions) {
            if (action.isEnabled()
                    && action.getActionType() == WorkflowActionType.CREATE_TASK) {
                throw new IllegalArgumentException(
                        "A task.created workflow cannot create another task"
                );
            }
        }
    }

    private void validateCondition(
            JsonNode condition,
            WebhookEventType eventType,
            int depth
    ) {
        if (depth > MAX_CONDITION_DEPTH) {
            throw new IllegalArgumentException(
                    "Workflow condition nesting cannot exceed five levels"
            );
        }
        requireObject(condition, "Workflow condition");

        boolean allGroup = condition.has("all");
        boolean anyGroup = condition.has("any");
        if (allGroup || anyGroup) {
            if (allGroup == anyGroup || condition.size() != 1) {
                throw new IllegalArgumentException(
                        "A condition group must contain only all or any"
                );
            }
            JsonNode children = condition.get(allGroup ? "all" : "any");
            if (!children.isArray()
                    || children.isEmpty()
                    || children.size() > MAX_GROUP_CONDITIONS) {
                throw new IllegalArgumentException(
                        "A condition group requires between one and ten items"
                );
            }
            for (JsonNode child : children) {
                validateCondition(child, eventType, depth + 1);
            }
            return;
        }

        rejectUnknownFields(
                condition,
                Set.of("field", "operator", "value"),
                "Workflow condition"
        );
        String field = requireText(condition, "field", 120);
        if (!EVENT_FIELDS.get(eventType).contains(field)) {
            throw new IllegalArgumentException(
                    "Condition field is not available for "
                            + eventType.getValue()
            );
        }
        String operator = requireText(condition, "operator", 20);
        if (!CONDITION_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException(
                    "Unsupported workflow condition operator"
            );
        }

        JsonNode value = condition.get("value");
        if ("EXISTS".equals(operator)) {
            if (value != null) {
                throw new IllegalArgumentException(
                        "EXISTS conditions cannot contain a value"
                );
            }
            return;
        }
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(
                    "Workflow condition value is required"
            );
        }
        if ("IN".equals(operator)) {
            if (!value.isArray()
                    || value.isEmpty()
                    || value.size() > MAX_IN_VALUES) {
                throw new IllegalArgumentException(
                        "IN conditions require between one and twenty values"
                );
            }
            for (JsonNode item : value) {
                requireScalar(item, "IN condition values");
            }
        } else {
            requireScalar(value, "Workflow condition value");
        }
    }

    private void validateCreateTask(
            JsonNode configuration,
            WebhookEventType eventType
    ) {
        rejectUnknownFields(
                configuration,
                TASK_CONFIGURATION_FIELDS,
                "CREATE_TASK configuration"
        );
        validateTemplate(
                requireText(configuration, "titleTemplate", 200),
                eventType
        );

        String priority = optionalText(configuration, "priority", 20);
        if (priority != null) {
            try {
                TaskPriority.valueOf(priority);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unsupported task priority"
                );
            }
        }
        validateInteger(configuration, "dueDateOffsetDays", 0, 365);
        validatePolicy(
                configuration,
                "assigneePolicy",
                ASSIGNEE_POLICIES
        );
        validatePolicy(
                configuration,
                "relationPolicy",
                RELATION_POLICIES
        );
    }

    private void validateNotification(
            JsonNode configuration,
            WebhookEventType eventType
    ) {
        rejectUnknownFields(
                configuration,
                NOTIFICATION_CONFIGURATION_FIELDS,
                "SEND_IN_APP_NOTIFICATION configuration"
        );
        validateTemplate(
                requireText(configuration, "titleTemplate", 150),
                eventType
        );
        validateTemplate(
                requireText(configuration, "messageTemplate", 500),
                eventType
        );
        validatePolicy(
                configuration,
                "recipientPolicy",
                RECIPIENT_POLICIES
        );
    }

    private void validateTemplate(
            String template,
            WebhookEventType eventType
    ) {
        Set<String> allowedTokens = EVENT_FIELDS.get(eventType).stream()
                .map(path -> path.substring("data.".length()))
                .collect(java.util.stream.Collectors.toSet());
        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        while (matcher.find()) {
            if (!allowedTokens.contains(matcher.group(1))) {
                throw new IllegalArgumentException(
                        "Template token is not available for "
                                + eventType.getValue()
                );
            }
        }
        String withoutValidTokens = matcher.reset().replaceAll("");
        if (withoutValidTokens.contains("{{")
                || withoutValidTokens.contains("}}")) {
            throw new IllegalArgumentException("Template token is malformed");
        }
    }

    private void validateInteger(
            JsonNode object,
            String field,
            int minimum,
            int maximum
    ) {
        JsonNode value = object.get(field);
        if (value == null) {
            return;
        }
        if (!value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.asInt() < minimum
                || value.asInt() > maximum) {
            throw new IllegalArgumentException(
                    field + " must be between " + minimum + " and " + maximum
            );
        }
    }

    private void validatePolicy(
            JsonNode object,
            String field,
            Set<String> allowedValues
    ) {
        String value = optionalText(object, field, 40);
        if (value != null && !allowedValues.contains(value)) {
            throw new IllegalArgumentException("Unsupported " + field);
        }
    }

    private String requireText(
            JsonNode object,
            String field,
            int maximumLength
    ) {
        JsonNode value = object.get(field);
        if (value == null
                || !value.isTextual()
                || value.asText().isBlank()
                || value.asText().length() > maximumLength) {
            throw new IllegalArgumentException(
                    field + " must be non-blank and at most "
                            + maximumLength + " characters"
            );
        }
        return value.asText();
    }

    private String optionalText(
            JsonNode object,
            String field,
            int maximumLength
    ) {
        JsonNode value = object.get(field);
        if (value == null) {
            return null;
        }
        if (!value.isTextual()
                || value.asText().isBlank()
                || value.asText().length() > maximumLength) {
            throw new IllegalArgumentException(
                    field + " must be non-blank and at most "
                            + maximumLength + " characters"
            );
        }
        return value.asText();
    }

    private void rejectUnknownFields(
            JsonNode object,
            Set<String> allowedFields,
            String label
    ) {
        for (String field : object.propertyNames()) {
            if (!allowedFields.contains(field)) {
                throw new IllegalArgumentException(
                        label + " contains unsupported field: " + field
                );
            }
        }
    }

    private void requireObject(JsonNode value, String label) {
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException(label + " must be an object");
        }
    }

    private void requireScalar(JsonNode value, String label) {
        if (!value.isValueNode()) {
            throw new IllegalArgumentException(label + " must be scalar");
        }
    }

    private String serializeBounded(JsonNode value, String label) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            if (serialized.getBytes(StandardCharsets.UTF_8).length
                    > MAX_JSON_BYTES) {
                throw new IllegalArgumentException(
                        label + " cannot exceed 16 KiB"
                );
            }
            return serialized;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(label + " is invalid");
        }
    }

    private static Map<WebhookEventType, Set<String>> eventFields() {
        Map<WebhookEventType, Set<String>> fields = new EnumMap<>(
                WebhookEventType.class
        );
        Set<String> customerFields = Set.of(
                "data.customer.id",
                "data.customer.name",
                "data.customer.companyName",
                "data.customer.customerType",
                "data.customer.status",
                "data.customer.ownerUserId"
        );
        Set<String> leadFields = Set.of(
                "data.lead.id",
                "data.lead.fullName",
                "data.lead.companyName",
                "data.lead.source",
                "data.lead.estimatedValue",
                "data.lead.status",
                "data.lead.assignedToUserId",
                "data.customer.id"
        );
        Set<String> contactFields = Set.of(
                "data.contact.id",
                "data.contact.customerId",
                "data.contact.fullName",
                "data.contact.position",
                "data.contact.status"
        );
        Set<String> taskFields = Set.of(
                "data.task.id",
                "data.task.title",
                "data.task.status",
                "data.task.priority",
                "data.task.dueDate",
                "data.task.assignedToUserId",
                "data.task.customerId",
                "data.task.leadId"
        );
        Set<String> noteFields = Set.of(
                "data.note.id",
                "data.note.customerId",
                "data.note.leadId",
                "data.note.createdByUserId"
        );

        fields.put(WebhookEventType.CUSTOMER_CREATED, customerFields);
        fields.put(WebhookEventType.CUSTOMER_UPDATED, customerFields);
        fields.put(WebhookEventType.CUSTOMER_ARCHIVED, customerFields);
        fields.put(WebhookEventType.CUSTOMER_RESTORED, customerFields);
        fields.put(WebhookEventType.LEAD_CREATED, leadFields);
        fields.put(WebhookEventType.LEAD_UPDATED, leadFields);
        fields.put(WebhookEventType.LEAD_ARCHIVED, leadFields);
        fields.put(WebhookEventType.LEAD_CONVERTED, leadFields);
        fields.put(WebhookEventType.CONTACT_CREATED, contactFields);
        fields.put(WebhookEventType.CONTACT_UPDATED, contactFields);
        fields.put(WebhookEventType.TASK_CREATED, taskFields);
        fields.put(WebhookEventType.TASK_UPDATED, taskFields);
        fields.put(WebhookEventType.TASK_COMPLETED, taskFields);
        fields.put(WebhookEventType.NOTE_CREATED, noteFields);
        return Map.copyOf(fields);
    }
}
