package com.crm.backend.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<CrmTask, Long> {
    @EntityGraph(attributePaths = {
            "assignedToUser",
            "assignedToUser.team",
            "customer",
            "lead"
    })
    @Query("""
            SELECT t FROM CrmTask t
            LEFT JOIN t.assignedToUser assignee
            LEFT JOIN assignee.team assigneeTeam
            WHERE t.organization.id = :organizationId
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR t.status = :status)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:assignedToUserId IS NULL OR t.assignedToUser.id = :assignedToUserId)
            AND (:customerId IS NULL OR t.customer.id = :customerId)
            AND (:leadId IS NULL OR t.lead.id = :leadId)
            AND (
                :allAccess = true
                OR assignee.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND assigneeTeam.id = :currentTeamId
                )
            )
            """)
    Page<CrmTask> searchAccessibleTasksInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("keyword") String keyword,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("assignedToUserId") Long assignedToUserId,
            @Param("customerId") Long customerId,
            @Param("leadId") Long leadId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "assignedToUser",
            "assignedToUser.team",
            "customer",
            "lead"
    })
    @Query("""
            SELECT t FROM CrmTask t
            LEFT JOIN t.assignedToUser assignee
            LEFT JOIN assignee.team assigneeTeam
            WHERE t.dueDate IS NOT NULL
            AND t.organization.id = :organizationId
            AND t.dueDate >= :from
            AND t.dueDate < :to
            AND (:assignedToUserId IS NULL
                 OR t.assignedToUser.id = :assignedToUserId)
            AND (
                :allAccess = true
                OR assignee.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND assigneeTeam.id = :currentTeamId
                )
            )
            ORDER BY t.dueDate ASC
            """)
    Page<CrmTask> findAccessibleCalendarTasksInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("assignedToUserId") Long assignedToUserId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "assignedToUser",
            "assignedToUser.team",
            "customer",
            "lead"
    })
    @Query("""
            SELECT t FROM CrmTask t
            LEFT JOIN t.assignedToUser assignee
            LEFT JOIN assignee.team assigneeTeam
            WHERE t.id = :id
            AND t.organization.id = :organizationId
            AND (
                :allAccess = true
                OR assignee.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND assigneeTeam.id = :currentTeamId
                )
            )
            """)
    Optional<CrmTask> findAccessibleByIdInOrganization(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );

    @Query("""
        SELECT COUNT(t) FROM CrmTask t
        LEFT JOIN t.assignedToUser assignee
        LEFT JOIN assignee.team assigneeTeam
        WHERE t.organization.id = :organizationId
        AND t.status = :status
        AND (
            :allAccess = true
            OR assignee.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND assigneeTeam.id = :currentTeamId
            )
        )
        """)
    long countAccessibleByStatusInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("status") TaskStatus status,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );

    @Query("""
        SELECT COUNT(t) FROM CrmTask t
        LEFT JOIN t.assignedToUser assignee
        LEFT JOIN assignee.team assigneeTeam
        WHERE t.organization.id = :organizationId
        AND t.dueDate IS NOT NULL
        AND t.dueDate < :now
        AND t.status NOT IN (com.crm.backend.task.TaskStatus.COMPLETED,
                             com.crm.backend.task.TaskStatus.CANCELLED)
        AND (
            :allAccess = true
            OR assignee.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND assigneeTeam.id = :currentTeamId
            )
        )
        """)
    long countAccessibleOverdueInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("now") LocalDateTime now,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
