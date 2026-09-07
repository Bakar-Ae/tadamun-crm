package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
import com.crm.backend.webhook.WebhookEvent;
import com.crm.backend.webhook.WebhookEventType;
import com.crm.backend.webhook.WebhookEventTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_execution_id", nullable = false, unique = true,
            length = 40, updatable = false)
    private String publicExecutionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false, updatable = false)
    private WorkflowDefinition workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_event_id", nullable = false, updatable = false)
    private WebhookEvent sourceEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_execution_id", updatable = false)
    private WorkflowExecution parentExecution;

    @Column(name = "workflow_version", nullable = false, updatable = false)
    private long workflowVersion;

    @Convert(converter = WebhookEventTypeConverter.class)
    @Column(name = "trigger_event_type", nullable = false, length = 100,
            updatable = false)
    private WebhookEventType triggerEventType;

    @Column(name = "source_event_public_id", nullable = false, length = 40,
            updatable = false)
    private String sourceEventPublicId;

    @Column(name = "correlation_id", nullable = false, length = 40,
            updatable = false)
    private String correlationId;

    @Column(name = "automation_depth", nullable = false, updatable = false,
            columnDefinition = "SMALLINT")
    private short automationDepth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowExecutionStatus status = WorkflowExecutionStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claim_token", length = 64)
    private String claimToken;

    @Column(name = "current_action_order", columnDefinition = "SMALLINT")
    private Short currentActionOrder;

    @Column(name = "input_payload", nullable = false, columnDefinition = "JSON",
            updatable = false)
    private String inputPayload;

    @Column(name = "last_error_category", length = 60)
    private String lastErrorCategory;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
