package com.crm.backend.integration.delivery;

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
@Table(name = "integration_delivery_attempts")
public class IntegrationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, updatable = false)
    private IntegrationDelivery delivery;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private IntegrationDeliveryOutcome outcome;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private int durationMs;

    @Column(name = "provider_message_id", length = 255, updatable = false)
    private String providerMessageId;

    @Column(name = "error_category", length = 60, updatable = false)
    private String errorCategory;

    @Column(name = "error_message", length = 500, updatable = false)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime attemptedAt;
}
