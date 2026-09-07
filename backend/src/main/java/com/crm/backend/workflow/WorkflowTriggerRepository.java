package com.crm.backend.workflow;

import com.crm.backend.webhook.WebhookEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTriggerRepository
        extends JpaRepository<WorkflowTrigger, Long> {

    Optional<WorkflowTrigger> findByOrganizationIdAndWorkflowId(
            Long organizationId,
            Long workflowId
    );

    List<WorkflowTrigger> findByOrganizationIdAndWorkflowIdIn(
            Long organizationId,
            List<Long> workflowIds
    );

    List<WorkflowTrigger> findByOrganizationIdAndEventTypeAndEnabled(
            Long organizationId,
            WebhookEventType eventType,
            boolean enabled
    );
}
