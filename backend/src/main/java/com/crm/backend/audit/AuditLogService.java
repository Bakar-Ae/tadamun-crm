package com.crm.backend.audit;

import com.crm.backend.audit.dto.AuditLogResponse;
import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
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
    private final CurrentOrganizationProvider currentOrganizationProvider;
    private String cleanFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            AuditLogMapper auditLogMapper,
            CurrentOrganizationProvider currentOrganizationProvider
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.auditLogMapper = auditLogMapper;
        this.currentOrganizationProvider = currentOrganizationProvider;
    }

    @Transactional
    public void log(Long actorUserId, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        Organization organization = currentOrganizationProvider
                .getOptionalOrganizationReference()
                .orElse(null);
        auditLog.setOrganization(organization);
        auditLog.setScope(
                organization == null
                        ? AuditLogScope.PLATFORM
                        : AuditLogScope.ORGANIZATION
        );
        auditLog.setActorUser(findUserOrNull(actorUserId));
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.searchAuditLogsInOrganization(
                currentOrganizationProvider.getOrganizationId(),
                null,
                null,
                null,
                null,
                pageable
        ).map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByOrganizationIdAndEntityType(
                        currentOrganizationProvider.getOrganizationId(),
                        entityType,
                        pageable
                )
                .map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByActor(Long actorUserId, Pageable pageable) {
        return auditLogRepository.findByOrganizationIdAndActorUserId(
                        currentOrganizationProvider.getOrganizationId(),
                        actorUserId,
                        pageable
                )
                .map(auditLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchAuditLogs(
            String action,
            String entityType,
            Long actorUserId,
            String keyword,
            Pageable pageable
    ) {
        String cleanAction = cleanFilter(action);
        String cleanEntityType = cleanFilter(entityType);
        String cleanKeyword = cleanFilter(keyword);

        return auditLogRepository.searchAuditLogsInOrganization(
                currentOrganizationProvider.getOrganizationId(),
                cleanAction,
                cleanEntityType,
                actorUserId,
                cleanKeyword,
                pageable
        ).map(auditLogMapper::toResponse);
    }

    private User findUserOrNull(Long actorUserId) {
        if (actorUserId == null) {
            return null;
        }

        return userRepository.findById(actorUserId).orElse(null);
    }
}
