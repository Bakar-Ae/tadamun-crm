package com.crm.backend.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecution, Long> {

    Page<WorkflowExecution> findByOrganizationIdAndWorkflowId(
            Long organizationId,
            Long workflowId,
            Pageable pageable
    );

    Optional<WorkflowExecution> findByOrganizationIdAndPublicExecutionId(
            Long organizationId,
            String publicExecutionId
    );
}
