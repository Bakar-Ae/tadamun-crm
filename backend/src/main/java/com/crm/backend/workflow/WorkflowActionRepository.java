package com.crm.backend.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowActionRepository
        extends JpaRepository<WorkflowAction, Long> {

    List<WorkflowAction>
    findByOrganizationIdAndWorkflowIdAndDefinitionVersionOrderByActionOrderAsc(
            Long organizationId,
            Long workflowId,
            long definitionVersion
    );

    List<WorkflowAction>
    findByOrganizationIdAndWorkflowIdInAndRetiredAtIsNullOrderByActionOrderAsc(
            Long organizationId,
            List<Long> workflowIds
    );
}
