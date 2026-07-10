package com.crm.backend.report;

import com.crm.backend.lead.LeadStatus;
import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskStatus;
import jakarta.persistence.EntityManager;
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

    public long countCustomersCreated(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        SELECT COUNT(customer)
                        FROM Customer customer
                        WHERE customer.createdAt >= :from
                          AND customer.createdAt < :to
                        """, Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public List<ReportBreakdownItem> countLeadsByStatus(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        SELECT lead.status, COUNT(lead)
                        FROM Lead lead
                        WHERE lead.createdAt >= :from
                          AND lead.createdAt < :to
                        GROUP BY lead.status
                        ORDER BY lead.status
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList()
                .stream()
                .map(row -> new ReportBreakdownItem(
                        ((LeadStatus) row[0]).name(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<ReportBreakdownItem> countTasksByStatus(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        SELECT task.status, COUNT(task)
                        FROM CrmTask task
                        WHERE task.createdAt >= :from
                          AND task.createdAt < :to
                        GROUP BY task.status
                        ORDER BY task.status
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList()
                .stream()
                .map(row -> new ReportBreakdownItem(
                        ((TaskStatus) row[0]).name(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    public List<ReportBreakdownItem> countTasksByPriority(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        SELECT task.priority, COUNT(task)
                        FROM CrmTask task
                        WHERE task.createdAt >= :from
                          AND task.createdAt < :to
                        GROUP BY task.priority
                        ORDER BY task.priority
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList()
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
            LocalDateTime to
    ) {
        return entityManager.createQuery("""
                        SELECT COUNT(auditLog)
                        FROM AuditLog auditLog
                        WHERE auditLog.action = :action
                          AND auditLog.createdAt >= :from
                          AND auditLog.createdAt < :to
                        """, Long.class)
                .setParameter("action", action)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countAuditEventsByEntityType(
            String entityType,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return entityManager.createQuery("""
                        SELECT COUNT(auditLog)
                        FROM AuditLog auditLog
                        WHERE auditLog.entityType = :entityType
                          AND auditLog.createdAt >= :from
                          AND auditLog.createdAt < :to
                        """, Long.class)
                .setParameter("entityType", entityType)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<ReportDailyActivity> countDailyActivity(
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT DATE(created_at), COUNT(*)
                        FROM audit_logs
                        WHERE created_at >= :from
                          AND created_at < :to
                        GROUP BY DATE(created_at)
                        ORDER BY DATE(created_at)
                        """)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        return rows.stream()
                .map(row -> new ReportDailyActivity(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();
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
