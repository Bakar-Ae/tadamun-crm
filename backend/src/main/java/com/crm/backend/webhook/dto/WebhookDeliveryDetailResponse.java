package com.crm.backend.webhook.dto;

import java.util.List;

public record WebhookDeliveryDetailResponse(
        WebhookDeliveryResponse delivery,
        List<WebhookDeliveryAttemptResponse> attempts
) {
}
