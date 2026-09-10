package com.crm.backend.workflow;

import com.crm.backend.webhook.WebhookEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT trigger
            FROM WorkflowTrigger trigger
            JOIN FETCH trigger.workflow workflow
            WHERE trigger.organization.id = :organizationId
              AND trigger.eventType = :eventType
              AND trigger.enabled = true
              AND workflow.status = :status
            ORDER BY workflow.id
            """)
    List<WorkflowTrigger> findMatchingTriggers(
            @Param("organizationId") Long organizationId,
            @Param("eventType") WebhookEventType eventType,
            @Param("status") WorkflowStatus status
    );
    @Query("""
        SELECT CASE WHEN COUNT(trigger) > 0 THEN true ELSE false END
        FROM WorkflowTrigger trigger
        JOIN trigger.workflow workflow
        WHERE trigger.organization.id = :organizationId
          AND trigger.eventType = :eventType
          AND trigger.enabled = true
          AND workflow.status = :status
        """)
    boolean existsMatchingTrigger(
            @Param("organizationId") Long organizationId,
            @Param("eventType") WebhookEventType eventType,
            @Param("status") WorkflowStatus status
    );
}
