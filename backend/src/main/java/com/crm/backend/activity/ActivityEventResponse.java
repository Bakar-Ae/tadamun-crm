package com.crm.backend.activity;

import java.time.LocalDateTime;

public record ActivityEventResponse(
        String eventKey,
        ActivityEventType type,
        Long sourceId,
        String title,
        String description,
        String actorName,
        String status,
        LocalDateTime occurredAt
) {
}