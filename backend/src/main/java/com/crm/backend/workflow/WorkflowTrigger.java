package com.crm.backend.workflow;

import com.crm.backend.organization.Organization;
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
@Table(name = "workflow_triggers")
public class WorkflowTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false, updatable = false)
    private WorkflowDefinition workflow;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private WorkflowTriggerType triggerType = WorkflowTriggerType.CRM_EVENT;

    @Convert(converter = WebhookEventTypeConverter.class)
    @Column(name = "event_type", nullable = false, length = 100)
    private WebhookEventType eventType;

    @Column(name = "condition_config", columnDefinition = "JSON")
    private String conditionConfig;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
