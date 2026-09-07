package com.crm.backend.webhook;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_subscription_events")
public class WebhookSubscriptionEvent {

    @EmbeddedId
    private WebhookSubscriptionEventId id;

    @MapsId("subscriptionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "subscription_id",
            nullable = false,
            updatable = false
    )
    private WebhookSubscription subscription;

    @Column(
            name = "organization_id",
            nullable = false,
            updatable = false
    )
    private Long organizationId;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}