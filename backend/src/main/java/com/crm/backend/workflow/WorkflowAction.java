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
@Table(name = "workflow_actions")
public class WorkflowAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_action_id", nullable = false, unique = true,
            length = 40, updatable = false)
    private String publicActionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false, updatable = false)
    private WorkflowDefinition workflow;

    @Column(name = "definition_version", nullable = false, updatable = false)
    private long definitionVersion;

    @Column(name = "action_order", nullable = false, updatable = false,
            columnDefinition = "SMALLINT")
    private short actionOrder;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private WorkflowActionType actionType;

    @Column(nullable = false, columnDefinition = "JSON")
    private String configuration;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 15;

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
