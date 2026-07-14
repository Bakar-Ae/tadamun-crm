package com.crm.backend.dashboard;

import com.crm.backend.audit.AuditLog;

import java.time.LocalDateTime;

public record TeamActivityResponse(
        Long id,
        Long actorUserId,
        String actorName,
        String action,
        String entityType,
        Long entityId,
        LocalDateTime createdAt
) {
    public static TeamActivityResponse from(AuditLog auditLog) {
        return new TeamActivityResponse(
                auditLog.getId(),
                auditLog.getActorUser() == null
                        ? null
                        : auditLog.getActorUser().getId(),
                auditLog.getActorUser() == null
                        ? "System"
                        : auditLog.getActorUser().getFullName(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getCreatedAt()
        );
    }
}
