package com.crm.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = {"actorUser"})
    Page<AuditLog> findByOrganizationIdAndEntityTypeAndEntityId(
            Long organizationId,
            String entityType,
            Long entityId,
            Pageable pageable
    );

    Page<AuditLog> findByOrganizationIdAndEntityType(
            Long organizationId,
            String entityType,
            Pageable pageable
    );

    Page<AuditLog> findByOrganizationIdAndActorUserId(
            Long organizationId,
            Long actorUserId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"actorUser", "actorUser.team"})
    @Query("""
            SELECT auditLog FROM AuditLog auditLog
            LEFT JOIN auditLog.actorUser actor
            LEFT JOIN actor.team actorTeam
            WHERE auditLog.organization.id = :organizationId
            AND auditLog.scope = com.crm.backend.audit.AuditLogScope.ORGANIZATION
            AND (
                :allAccess = true
                OR actor.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND actorTeam.id = :currentTeamId
                )
            )
            ORDER BY auditLog.id DESC
            """)
    Page<AuditLog> findAccessibleRecentActivityInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @Query("""
            SELECT auditLog
            FROM AuditLog auditLog
            WHERE auditLog.organization.id = :organizationId
              AND auditLog.scope = com.crm.backend.audit.AuditLogScope.ORGANIZATION
              AND (:action is null or auditLog.action = :action)
              AND (:entityType is null or auditLog.entityType = :entityType)
              AND (:actorUserId is null or auditLog.actorUser.id = :actorUserId)
              AND (
                    :keyword is null
                    or lower(auditLog.action) like lower(concat('%', :keyword, '%'))
                    or lower(auditLog.entityType) like lower(concat('%', :keyword, '%'))
                    or lower(auditLog.details) like lower(concat('%', :keyword, '%'))
                    or lower(auditLog.actorUser.fullName) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<AuditLog> searchAuditLogsInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("actorUserId") Long actorUserId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
