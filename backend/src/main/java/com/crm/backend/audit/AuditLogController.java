package com.crm.backend.audit;

import com.crm.backend.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(pageable));
    }

    @GetMapping("/entity/{entityType}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntityType(
            @PathVariable String entityType,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByEntityType(entityType, pageable));
    }

    @GetMapping("/actor/{actorUserId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByActor(
            @PathVariable Long actorUserId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByActor(actorUserId, pageable));
    }
}