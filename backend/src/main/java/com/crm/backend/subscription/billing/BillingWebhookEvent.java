package com.crm.backend.subscription.billing;

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
@Table(name = "billing_webhook_events")
public class BillingWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillingProviderName provider;

    @Column(name = "provider_event_id", nullable = false, length = 255)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private BillingWebhookProcessingStatus processingStatus;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(
            name = "received_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}