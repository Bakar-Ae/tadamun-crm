package com.crm.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUserName,
        String action,
        String entityType,
        Long entityId,
        String details,
        LocalDateTime createdAt
) {
}