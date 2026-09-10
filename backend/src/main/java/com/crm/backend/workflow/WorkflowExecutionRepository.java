package com.crm.backend.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecution, Long> {

    Page<WorkflowExecution> findByOrganizationIdAndWorkflowId(
            Long organizationId,
            Long workflowId,
            Pageable pageable
    );

    Page<WorkflowExecution> findByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId,
            Pageable pageable
    );

    Optional<WorkflowExecution> findByOrganizationIdAndPublicExecutionId(
            Long organizationId,
            String publicExecutionId
    );

    boolean existsByOrganizationIdAndWorkflowIdAndSourceEventId(
            Long organizationId,
            Long workflowId,
            Long sourceEventId
    );

    long countByOrganizationIdAndWorkflowIdAndCreatedAtGreaterThanEqual(
            Long organizationId,
            Long workflowId,
            LocalDateTime createdAfter
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT execution
            FROM WorkflowExecution execution
            JOIN FETCH execution.workflow
            WHERE execution.organization.id = :organizationId
              AND execution.publicExecutionId = :publicExecutionId
            """)
    Optional<WorkflowExecution> findForUpdateByPublicId(
            @Param("organizationId") Long organizationId,
            @Param("publicExecutionId") String publicExecutionId
    );
}
