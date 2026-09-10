package com.crm.backend.workflow;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkflowActionExecutionRepository
        extends JpaRepository<WorkflowActionExecution, Long> {

    List<WorkflowActionExecution>
    findByOrganizationIdAndExecutionIdOrderByActionOrderAsc(
            Long organizationId,
            Long executionId
    );

    Optional<WorkflowActionExecution>
    findByOrganizationIdAndPublicActionExecutionId(
            Long organizationId,
            String publicActionExecutionId
    );

    @Query("""
            SELECT actionExecution.id
            FROM WorkflowActionExecution actionExecution
            WHERE actionExecution.status IN :statuses
              AND actionExecution.nextAttemptAt <= :now
            ORDER BY actionExecution.nextAttemptAt, actionExecution.id
            """)
    List<Long> findClaimableIds(
            @Param("statuses") List<WorkflowActionExecutionStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "organization",
            "execution",
            "execution.workflow",
            "execution.sourceEvent",
            "action"
    })
    @Query("""
            SELECT actionExecution
            FROM WorkflowActionExecution actionExecution
            WHERE actionExecution.id = :id
            """)
    Optional<WorkflowActionExecution> findForUpdate(@Param("id") Long id);

    @Query("""
            SELECT actionExecution.id
            FROM WorkflowActionExecution actionExecution
            WHERE actionExecution.status = :status
              AND actionExecution.claimedAt < :staleBefore
            ORDER BY actionExecution.claimedAt, actionExecution.id
            """)
    List<Long> findStaleIds(
            @Param("status") WorkflowActionExecutionStatus status,
            @Param("staleBefore") LocalDateTime staleBefore,
            Pageable pageable
    );
}
