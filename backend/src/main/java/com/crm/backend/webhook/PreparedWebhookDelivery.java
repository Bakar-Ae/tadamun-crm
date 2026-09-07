package com.crm.backend.webhook;

import java.net.URI;
import java.util.List;

public final class PreparedWebhookDelivery {

    private final Long deliveryId;
    private final int attemptNumber;
    private final URI endpoint;
    private final String publicDeliveryId;
    private final String publicEventId;
    private final String eventType;
    private final byte[] body;
    private final List<String> signingSecrets;
    private final long requestTimestamp;

    public PreparedWebhookDelivery(
            Long deliveryId,
            int attemptNumber,
            URI endpoint,
            String publicDeliveryId,
            String publicEventId,
            String eventType,
            byte[] body,
            List<String> signingSecrets,
            long requestTimestamp
    ) {
        this.deliveryId = deliveryId;
        this.attemptNumber = attemptNumber;
        this.endpoint = endpoint;
        this.publicDeliveryId = publicDeliveryId;
        this.publicEventId = publicEventId;
        this.eventType = eventType;
        this.body = body.clone();
        this.signingSecrets = List.copyOf(signingSecrets);
        this.requestTimestamp = requestTimestamp;
    }

    public Long deliveryId() {
        return deliveryId;
    }

    public int attemptNumber() {
        return attemptNumber;
    }

    public URI endpoint() {
        return endpoint;
    }

    public String publicDeliveryId() {
        return publicDeliveryId;
    }

    public String publicEventId() {
        return publicEventId;
    }

    public String eventType() {
        return eventType;
    }

    public byte[] body() {
        return body.clone();
    }

    public List<String> signingSecrets() {
        return signingSecrets;
    }

    public long requestTimestamp() {
        return requestTimestamp;
    }

    @Override
    public String toString() {
        return "PreparedWebhookDelivery[deliveryId=" + deliveryId
                + ", publicDeliveryId=" + publicDeliveryId
                + ", publicEventId=" + publicEventId
                + ", eventType=" + eventType
                + ", body=<redacted>, signingSecrets=<redacted>]";
    }
}
