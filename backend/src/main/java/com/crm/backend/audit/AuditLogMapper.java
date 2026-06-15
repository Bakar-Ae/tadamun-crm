package com.crm.backend.audit;

import com.crm.backend.audit.dto.AuditLogResponse;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUser() == null ? null : auditLog.getActorUser().getId(),
                auditLog.getActorUser() == null ? null : auditLog.getActorUser().getFullName(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}