package com.crm.backend.workflow;

import com.crm.backend.webhook.WebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowConfigurationValidatorTest {

    private ObjectMapper objectMapper;
    private WorkflowConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new WorkflowConfigurationValidator(objectMapper);
    }

    @Test
    void shouldAcceptAllowlistedConditionAndTaskConfiguration()
            throws Exception {
        JsonNode condition = json("""
                {
                  "all": [
                    {
                      "field": "data.customer.status",
                      "operator": "EQUALS",
                      "value": "ACTIVE"
                    },
                    {
                      "field": "data.customer.ownerUserId",
                      "operator": "EXISTS"
                    }
                  ]
                }
                """);
        JsonNode configuration = json("""
                {
                  "titleTemplate": "Follow up {{customer.name}}",
                  "priority": "HIGH",
                  "dueDateOffsetDays": 2,
                  "assigneePolicy": "RECORD_OWNER_OR_ASSIGNEE",
                  "relationPolicy": "TRIGGER_RECORD"
                }
                """);

        assertDoesNotThrow(() -> validator.validateAndSerializeCondition(
                WebhookEventType.CUSTOMER_CREATED,
                condition
        ));
        assertDoesNotThrow(() -> validator.validateAndSerializeAction(
                WebhookEventType.CUSTOMER_CREATED,
                WorkflowActionType.CREATE_TASK,
                configuration
        ));
    }

    @Test
    void shouldRejectIdentifiersAndUnknownConfigurationFields()
            throws Exception {
        JsonNode configuration = json("""
                {
                  "titleTemplate": "Unsafe task",
                  "organizationId": 99
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateAndSerializeAction(
                        WebhookEventType.CUSTOMER_CREATED,
                        WorkflowActionType.CREATE_TASK,
                        configuration
                )
        );
    }

    @Test
    void shouldRejectConditionFieldFromAnotherEvent() throws Exception {
        JsonNode condition = json("""
                {
                  "field": "data.lead.estimatedValue",
                  "operator": "EQUALS",
                  "value": 1000
                }
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateAndSerializeCondition(
                        WebhookEventType.CUSTOMER_CREATED,
                        condition
                )
        );
    }

    @Test
    void shouldRejectDirectTaskCreationLoop() {
        WorkflowAction action = new WorkflowAction();
        action.setEnabled(true);
        action.setActionType(WorkflowActionType.CREATE_TASK);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.rejectDirectLoop(
                        WebhookEventType.TASK_CREATED,
                        java.util.List.of(action)
                )
        );
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
