package com.crm.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByActorUserId(Long actorUserId, Pageable pageable);

    @Query("""
            select auditLog
            from AuditLog auditLog
            where (:action is null or auditLog.action = :action)
              and (:entityType is null or auditLog.entityType = :entityType)
              and (:actorUserId is null or auditLog.actorUser.id = :actorUserId)
              and (
                    :keyword is null
                    or lower(auditLog.action) like lower(concat('%', :keyword, '%'))
                    or lower(auditLog.entityType) like lower(concat('%', :keyword, '%'))
                    or lower(auditLog.details) like lower(concat('%', :keyword, '%'))
                    or lower(auditLog.actorUser.fullName) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("actorUserId") Long actorUserId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}