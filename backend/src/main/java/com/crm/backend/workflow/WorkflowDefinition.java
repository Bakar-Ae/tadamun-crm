package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
import com.crm.backend.user.User;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_workflow_id", nullable = false, unique = true,
            length = 40, updatable = false)
    private String publicWorkflowId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_policy", nullable = false, length = 30)
    private WorkflowFailurePolicy failurePolicy =
            WorkflowFailurePolicy.STOP_ON_FAILURE;

    @Column(name = "execution_timeout_seconds", nullable = false)
    private int executionTimeoutSeconds = 60;

    @Column(name = "maximum_attempts", nullable = false)
    private int maximumAttempts = 3;

    @Column(name = "maximum_executions_per_hour", nullable = false)
    private int maximumExecutionsPerHour = 100;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false,
            updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;

    @Column(name = "definition_version", nullable = false)
    private long definitionVersion = 1;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
