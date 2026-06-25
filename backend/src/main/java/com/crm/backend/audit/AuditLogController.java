package com.crm.backend.audit;

import com.crm.backend.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                auditLogService.searchAuditLogs(action, entityType, actorUserId, keyword, pageable)
        );
    }

    @GetMapping("/entity/{entityType}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntityType(
            @PathVariable String entityType,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByEntityType(entityType, pageable));
    }

    @GetMapping("/actor/{actorUserId}")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByActor(
            @PathVariable Long actorUserId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByActor(actorUserId, pageable));
    }
}
