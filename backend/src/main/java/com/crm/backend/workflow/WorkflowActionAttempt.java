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
@Table(name = "workflow_action_attempts")
public class WorkflowActionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_execution_id", nullable = false,
            updatable = false)
    private WorkflowActionExecution actionExecution;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private WorkflowActionAttemptOutcome outcome;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private int durationMs;

    @Column(name = "result_summary", columnDefinition = "JSON",
            updatable = false)
    private String resultSummary;

    @Column(name = "error_category", length = 60, updatable = false)
    private String errorCategory;

    @Column(name = "error_message", length = 500, updatable = false)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime attemptedAt;
}
