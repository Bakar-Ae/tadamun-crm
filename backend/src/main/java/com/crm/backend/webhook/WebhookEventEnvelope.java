package com.crm.backend.webhook;

import java.util.Map;

public record WebhookEventEnvelope(
        String id,
        int schemaVersion,
        String type,
        String occurredAt,
        String organizationId,
        Map<String, ?> data
) {
}
