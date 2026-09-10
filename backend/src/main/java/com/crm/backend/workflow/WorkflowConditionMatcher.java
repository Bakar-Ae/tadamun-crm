package com.crm.backend.workflow;

import com.crm.backend.webhook.WebhookEventType;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class WorkflowConditionMatcher {

    private final ObjectMapper objectMapper;
    private final WorkflowConfigurationValidator validator;

    public WorkflowConditionMatcher(
            ObjectMapper objectMapper,
            WorkflowConfigurationValidator validator
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public boolean matches(
            WebhookEventType eventType,
            String conditionJson,
            String eventPayload
    ) {
        if (conditionJson == null || conditionJson.isBlank()) {
            return true;
        }
        if (eventPayload == null || eventPayload.isBlank()) {
            return false;
        }

        try {
            JsonNode condition = objectMapper.readTree(conditionJson);
            JsonNode payload = objectMapper.readTree(eventPayload);
            validator.validateAndSerializeCondition(eventType, condition);
            return evaluate(condition, payload);
        } catch (JacksonException | IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean evaluate(JsonNode condition, JsonNode payload) {
        if (condition.has("all")) {
            for (JsonNode child : condition.get("all")) {
                if (!evaluate(child, payload)) {
                    return false;
                }
            }
            return true;
        }

        if (condition.has("any")) {
            for (JsonNode child : condition.get("any")) {
                if (evaluate(child, payload)) {
                    return true;
                }
            }
            return false;
        }

        JsonNode actual = resolve(payload, condition.get("field").asText());
        String operator = condition.get("operator").asText();

        if ("EXISTS".equals(operator)) {
            return actual != null && !actual.isNull();
        }
        if (actual == null || actual.isNull()) {
            return false;
        }

        JsonNode expected = condition.get("value");
        return switch (operator) {
            case "EQUALS" -> actual.equals(expected);
            case "NOT_EQUALS" -> !actual.equals(expected);
            case "IN" -> contains(expected, actual);
            default -> false;
        };
    }

    private boolean contains(JsonNode values, JsonNode actual) {
        for (JsonNode value : values) {
            if (actual.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode resolve(JsonNode payload, String path) {
        JsonNode current = payload;
        for (String segment : path.split("\\.")) {
            if (current == null || !current.isObject()) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }
}