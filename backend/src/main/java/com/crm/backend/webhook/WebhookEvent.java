package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_event_id", nullable = false, unique = true,
            length = 40, updatable = false)
    private String publicEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Convert(converter = WebhookEventTypeConverter.class)
    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private WebhookEventType eventType;

    @Column(name = "schema_version", nullable = false, updatable = false)
    private int schemaVersion = 1;

    @Column(name = "aggregate_type", nullable = false, length = 50, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private Long aggregateId;

    @Column(nullable = false, columnDefinition = "JSON", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private WebhookPublicationStatus publicationStatus =
            WebhookPublicationStatus.PENDING;

    @Column(name = "publication_attempts", nullable = false)
    private int publicationAttempts;

    @Column(name = "next_publication_attempt_at", nullable = false)
    private LocalDateTime nextPublicationAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claim_token", length = 64)
    private String claimToken;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;
}