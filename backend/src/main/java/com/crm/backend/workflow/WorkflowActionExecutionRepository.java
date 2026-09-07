package com.crm.backend.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
