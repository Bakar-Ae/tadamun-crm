package com.crm.backend.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowActionAttemptRepository
        extends JpaRepository<WorkflowActionAttempt, Long> {

    List<WorkflowActionAttempt>
    findByOrganizationIdAndActionExecutionIdOrderByAttemptNumberAsc(
            Long organizationId,
            Long actionExecutionId
    );
}
