package com.crm.backend.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class WebhookSubscriptionEventId implements Serializable {

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Convert(converter = WebhookEventTypeConverter.class)
    @Column(name = "event_type", nullable = false, length = 100)
    private WebhookEventType eventType;
}