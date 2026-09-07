package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
import jakarta.persistence.Column;
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
@Table(name = "workflow_action_executions")
public class WorkflowActionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_action_execution_id", nullable = false,
            unique = true, length = 40, updatable = false)
    private String publicActionExecutionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false, updatable = false)
    private WorkflowExecution execution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false, updatable = false)
    private WorkflowAction action;

    @Column(name = "action_order", nullable = false, updatable = false,
            columnDefinition = "SMALLINT")
    private short actionOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40,
            updatable = false)
    private WorkflowActionType actionType;

    @Column(name = "configuration_snapshot", nullable = false,
            columnDefinition = "JSON", updatable = false)
    private String configurationSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowActionExecutionStatus status =
            WorkflowActionExecutionStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claim_token", length = 64)
    private String claimToken;

    @Column(name = "idempotency_key", nullable = false, unique = true,
            length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "result_resource_type", length = 50)
    private String resultResourceType;

    @Column(name = "result_resource_id")
    private Long resultResourceId;

    @Column(name = "result_summary", columnDefinition = "JSON")
    private String resultSummary;

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
