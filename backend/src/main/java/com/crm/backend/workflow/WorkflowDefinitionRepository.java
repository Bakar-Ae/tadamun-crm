package com.crm.backend.workflow;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkflowDefinitionRepository
        extends JpaRepository<WorkflowDefinition, Long> {

    @EntityGraph(attributePaths = {"createdByUser", "updatedByUser"})
    Page<WorkflowDefinition> findByOrganizationId(
            Long organizationId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdByUser", "updatedByUser"})
    Optional<WorkflowDefinition> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    boolean existsByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(
            Long organizationId,
            String name
    );

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
            Long organizationId,
            String name,
            Long id
    );

    long countByOrganizationIdAndStatus(
            Long organizationId,
            WorkflowStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"createdByUser", "updatedByUser"})
    @Query("""
            SELECT workflow
            FROM WorkflowDefinition workflow
            WHERE workflow.id = :id
              AND workflow.organization.id = :organizationId
            """)
    Optional<WorkflowDefinition> findForUpdate(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId
    );
}
