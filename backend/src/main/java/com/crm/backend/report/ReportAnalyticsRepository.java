package com.crm.backend.report;

import com.crm.backend.lead.LeadStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReportAnalyticsRepository {

    private final EntityManager entityManager;

    public ReportAnalyticsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countCustomersCreated(
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        TypedQuery<Long> query = entityManager.createQuery("""
                        SELECT COUNT(customer)
                        FROM Customer customer
                        LEFT JOIN customer.ownerUser owner
                        LEFT JOIN owner.team ownerTeam
                        WHERE customer.organization.id = :organizationId
                          AND customer.createdAt >= :from
                          AND customer.createdAt < :to
                          AND (
                              :allAccess = true
                              OR owner.id = :currentUserId
                              OR (
                                  :teamAccess = true
                                  AND :currentTeamId IS NOT NULL
                                  AND ownerTeam.id = :currentTeamId
                              )
                          )
                        """, Long.class)
                .setParameter("from", from)
                .setParameter("to", to);

        return applyScope(query, context, organizationId).getSingleResult();
    }

    public List<ReportBreakdownItem> countLeadsByStatus(
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        TypedQuery<Object[]> query = entityManager.createQuery("""
                        SELECT lead.status, COUNT(lead)
                        FROM Lead lead
                        LEFT JOIN lead.assignedToUser assignee
                        LEFT JOIN assignee.team assigneeTeam
                        WHERE lead.organization.id = :organizationId
                          AND lead.createdAt >= :from
                          AND lead.createdAt < :to
                          AND (
                              :allAccess = true
                              OR assignee.id = :currentUserId
                              OR (
                                  :teamAccess = true
                                  AND :currentTeamId IS NOT NULL
                                  AND assigneeTeam.id = :currentTeamId
                              )
                          )
                        GROUP BY lead.status
                        ORDER BY lead.status
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to);

        return applyScope(query, context, organizationId).getResultList()
                .stream()
                .map(row -> new ReportBreakdownItem(
                        ((LeadStatus) row[0]).name(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<ReportBreakdownItem> countTasksByStatus(
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        TypedQuery<Object[]> query = entityManager.createQuery("""
                        SELECT task.status, COUNT(task)
                        FROM CrmTask task
                        LEFT JOIN task.assignedToUser assignee
                        LEFT JOIN assignee.team assigneeTeam
                        WHERE task.organization.id = :organizationId
                          AND task.createdAt >= :from
                          AND task.createdAt < :to
                          AND (
                              :allAccess = true
                              OR assignee.id = :currentUserId
                              OR (
                                  :teamAccess = true
                                  AND :currentTeamId IS NOT NULL
                                  AND assigneeTeam.id = :currentTeamId
                              )
                          )
                        GROUP BY task.status
                        ORDER BY task.status
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to);

        return applyScope(query, context, organizationId).getResultList()
                .stream()
                .map(row -> new ReportBreakdownItem(
                        ((TaskStatus) row[0]).name(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<ReportBreakdownItem> countTasksByPriority(
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        TypedQuery<Object[]> query = entityManager.createQuery("""
                        SELECT task.priority, COUNT(task)
                        FROM CrmTask task
                        LEFT JOIN task.assignedToUser assignee
                        LEFT JOIN assignee.team assigneeTeam
                        WHERE task.organization.id = :organizationId
                          AND task.createdAt >= :from
                          AND task.createdAt < :to
                          AND (
                              :allAccess = true
                              OR assignee.id = :currentUserId
                              OR (
                                  :teamAccess = true
                                  AND :currentTeamId IS NOT NULL
                                  AND assigneeTeam.id = :currentTeamId
                              )
                          )
                        GROUP BY task.priority
                        ORDER BY task.priority
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to);

        return applyScope(query, context, organizationId).getResultList()
                .stream()
                .map(row -> new ReportBreakdownItem(
                        ((TaskPriority) row[0]).name(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public long countAuditEventsByAction(
            String action,
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        TypedQuery<Long> query = entityManager.createQuery("""
                        SELECT COUNT(auditLog)
                        FROM AuditLog auditLog
                        LEFT JOIN auditLog.actorUser actor
                        LEFT JOIN actor.team actorTeam
                        WHERE auditLog.organization.id = :organizationId
                          AND auditLog.scope = com.crm.backend.audit.AuditLogScope.ORGANIZATION
                          AND auditLog.action = :action
                          AND auditLog.createdAt >= :from
                          AND auditLog.createdAt < :to
                          AND (
                              :allAccess = true
                              OR actor.id = :currentUserId
                              OR (
                                  :teamAccess = true
                                  AND :currentTeamId IS NOT NULL
                                  AND actorTeam.id = :currentTeamId
                              )
                          )
                        """, Long.class)
                .setParameter("action", action)
                .setParameter("from", from)
                .setParameter("to", to);

        return applyScope(query, context, organizationId).getSingleResult();
    }

    public long countAuditEventsByEntityType(
            String entityType,
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        TypedQuery<Long> query = entityManager.createQuery("""
                        SELECT COUNT(auditLog)
                        FROM AuditLog auditLog
                        LEFT JOIN auditLog.actorUser actor
                        LEFT JOIN actor.team actorTeam
                        WHERE auditLog.organization.id = :organizationId
                          AND auditLog.scope = com.crm.backend.audit.AuditLogScope.ORGANIZATION
                          AND auditLog.entityType = :entityType
                          AND auditLog.createdAt >= :from
                          AND auditLog.createdAt < :to
                          AND (
                              :allAccess = true
                              OR actor.id = :currentUserId
                              OR (
                                  :teamAccess = true
                                  AND :currentTeamId IS NOT NULL
                                  AND actorTeam.id = :currentTeamId
                              )
                          )
                        """, Long.class)
                .setParameter("entityType", entityType)
                .setParameter("from", from)
                .setParameter("to", to);

        return applyScope(query, context, organizationId).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<ReportDailyActivity> countDailyActivity(
            LocalDateTime from,
            LocalDateTime to,
            DataScopeContext context,
            Long organizationId
    ) {
        Query query = entityManager.createNativeQuery("""
                        SELECT DATE(a.created_at), COUNT(*)
                        FROM audit_logs a
                        LEFT JOIN users actor ON actor.id = a.actor_user_id
                        WHERE a.organization_id = :organizationId
                          AND a.scope = 'ORGANIZATION'
                          AND a.created_at >= :from
                          AND a.created_at < :to
                          AND (
                              :allAccess = TRUE
                              OR actor.id = :currentUserId
                              OR (
                                  :teamAccess = TRUE
                                  AND :currentTeamId IS NOT NULL
                                  AND actor.team_id = :currentTeamId
                              )
                          )
                        GROUP BY DATE(a.created_at)
                        ORDER BY DATE(a.created_at)
                        """)
                .setParameter("from", from)
                .setParameter("to", to);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = applyScope(
                query,
                context,
                organizationId
        ).getResultList();

        return rows.stream()
                .map(row -> new ReportDailyActivity(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    private <T> TypedQuery<T> applyScope(
            TypedQuery<T> query,
            DataScopeContext context,
            Long organizationId
    ) {
        query.setParameter("organizationId", organizationId);
        query.setParameter("allAccess", context.scope() == DataScope.ALL);
        query.setParameter("teamAccess", context.scope() == DataScope.TEAM);
        query.setParameter("currentUserId", context.userId());
        query.setParameter("currentTeamId", context.teamId());
        return query;
    }

    private Query applyScope(
            Query query,
            DataScopeContext context,
            Long organizationId
    ) {
        query.setParameter("organizationId", organizationId);
        query.setParameter("allAccess", context.scope() == DataScope.ALL);
        query.setParameter("teamAccess", context.scope() == DataScope.TEAM);
        query.setParameter("currentUserId", context.userId());
        query.setParameter("currentTeamId", context.teamId());
        return query;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return LocalDate.parse(value.toString());
    }
}
