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
@Table(name = "webhook_delivery_attempts")
public class WebhookDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            updatable = false
    )
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "delivery_id",
            nullable = false,
            updatable = false
    )
    private WebhookDelivery delivery;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private WebhookDeliveryOutcome outcome;

    @Column(name = "request_timestamp", nullable = false, updatable = false)
    private long requestTimestamp;

    @Column(name = "http_status", updatable = false)
    private Integer httpStatus;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private int durationMs;

    @Column(
            name = "response_excerpt",
            columnDefinition = "TEXT",
            updatable = false
    )
    private String responseExcerpt;

    @Column(name = "error_category", length = 60, updatable = false)
    private String errorCategory;

    @Column(name = "error_message", length = 500, updatable = false)
    private String errorMessage;

    @Column(
            name = "attempted_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime attemptedAt;
}