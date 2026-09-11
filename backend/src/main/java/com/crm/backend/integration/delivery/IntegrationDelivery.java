package com.crm.backend.integration.delivery;

import com.crm.backend.integration.IntegrationConnection;
import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "integration_deliveries")
public class IntegrationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_delivery_id", nullable = false, unique = true,
            length = 40, updatable = false)
    private String publicDeliveryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false, updatable = false)
    private IntegrationConnection connection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private IntegrationProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 30,
            updatable = false)
    private IntegrationDeliveryType deliveryType;

    @Column(nullable = false, length = 320, updatable = false)
    private String destination;

    @Column(length = 200, updatable = false)
    private String subject;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT",
            updatable = false)
    private String messageBody;

    @Column(name = "idempotency_key", nullable = false, length = 100,
            updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IntegrationDeliveryStatus status = IntegrationDeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "maximum_attempts", nullable = false)
    private int maximumAttempts = 6;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claim_token", length = 64)
    private String claimToken;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "last_error_category", length = 60)
    private String lastErrorCategory;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", updatable = false)
    private User createdByUser;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
