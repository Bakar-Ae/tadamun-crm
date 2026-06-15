package com.crm.backend.audit;

import com.crm.backend.audit.dto.AuditLogResponse;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            AuditLogMapper auditLogMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional
    public void log(Long actorUserId, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUser(findUserOrNull(actorUserId));
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable)
                .map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByActor(Long actorUserId, Pageable pageable) {
        return auditLogRepository.findByActorUserId(actorUserId, pageable)
                .map(auditLogMapper::toResponse);
    }

    private User findUserOrNull(Long actorUserId) {
        if (actorUserId == null) {
            return null;
        }

        return userRepository.findById(actorUserId).orElse(null);
    }
}