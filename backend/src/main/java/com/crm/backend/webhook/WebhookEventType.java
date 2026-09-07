package com.crm.backend.webhook;

import java.util.Arrays;

public enum WebhookEventType {
    CUSTOMER_CREATED("customer.created"),
    CUSTOMER_UPDATED("customer.updated"),
    CUSTOMER_ARCHIVED("customer.archived"),
    CUSTOMER_RESTORED("customer.restored"),
    LEAD_CREATED("lead.created"),
    LEAD_UPDATED("lead.updated"),
    LEAD_ARCHIVED("lead.archived"),
    LEAD_CONVERTED("lead.converted"),
    CONTACT_CREATED("contact.created"),
    CONTACT_UPDATED("contact.updated"),
    TASK_CREATED("task.created"),
    TASK_UPDATED("task.updated"),
    TASK_COMPLETED("task.completed"),
    NOTE_CREATED("note.created");

    private final String value;

    WebhookEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public static WebhookEventType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Webhook event type is required");
        }

        return Arrays.stream(values())
                .filter(type -> type.value.equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown webhook event type: " + value
                ));
    }
}