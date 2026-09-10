package com.crm.backend.workflow;

import com.crm.backend.webhook.WebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowConditionMatcherTest {

    private WorkflowConditionMatcher matcher;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        matcher = new WorkflowConditionMatcher(
                objectMapper,
                new WorkflowConfigurationValidator(objectMapper)
        );
    }

    @Test
    void shouldMatchNestedSupportedConditions() {
        String condition = """
                {
                  "all": [
                    {
                      "field": "data.customer.status",
                      "operator": "EQUALS",
                      "value": "ACTIVE"
                    },
                    {
                      "field": "data.customer.customerType",
                      "operator": "IN",
                      "value": ["BUSINESS", "INDIVIDUAL"]
                    },
                    {
                      "field": "data.customer.ownerUserId",
                      "operator": "EXISTS"
                    }
                  ]
                }
                """;

        String payload = """
                {
                  "data": {
                    "customer": {
                      "status": "ACTIVE",
                      "customerType": "BUSINESS",
                      "ownerUserId": 7
                    }
                  }
                }
                """;

        assertTrue(matcher.matches(
                WebhookEventType.CUSTOMER_CREATED,
                condition,
                payload
        ));
    }

    @Test
    void shouldFailClosedForInvalidOrMissingValues() {
        assertFalse(matcher.matches(
                WebhookEventType.CUSTOMER_CREATED,
                "{invalid-json",
                "{}"
        ));

        assertFalse(matcher.matches(
                WebhookEventType.CUSTOMER_CREATED,
                """
                {
                  "field": "data.customer.ownerUserId",
                  "operator": "EXISTS"
                }
                """,
                """
                {"data":{"customer":{"status":"ACTIVE"}}}
                """
        ));
    }
}