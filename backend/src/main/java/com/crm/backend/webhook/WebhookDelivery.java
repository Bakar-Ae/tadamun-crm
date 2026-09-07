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
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_delivery_id", nullable = false,
            unique = true, length = 40, updatable = false)
    private String publicDeliveryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private WebhookEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false, updatable = false)
    private WebhookSubscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookDeliveryStatus status = WebhookDeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claim_token", length = 64)
    private String claimToken;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_duration_ms")
    private Integer lastDurationMs;

    @Column(name = "last_error_category", length = 60)
    private String lastErrorCategory;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "last_response_excerpt", columnDefinition = "TEXT")
    private String lastResponseExcerpt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}