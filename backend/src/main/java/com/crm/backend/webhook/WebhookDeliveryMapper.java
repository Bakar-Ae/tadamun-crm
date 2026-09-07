package com.crm.backend.webhook;

import com.crm.backend.webhook.dto.WebhookDeliveryAttemptResponse;
import com.crm.backend.webhook.dto.WebhookDeliveryDetailResponse;
import com.crm.backend.webhook.dto.WebhookDeliveryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebhookDeliveryMapper {

    public WebhookDeliveryResponse toResponse(WebhookDelivery delivery) {
        return new WebhookDeliveryResponse(
                delivery.getPublicDeliveryId(),
                delivery.getEvent().getPublicEventId(),
                delivery.getEvent().getEventType().getValue(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getNextAttemptAt(),
                delivery.getLastHttpStatus(),
                delivery.getLastDurationMs(),
                delivery.getLastErrorCategory(),
                delivery.getLastError(),
                delivery.getCompletedAt(),
                delivery.getEvent().getOccurredAt(),
                delivery.getCreatedAt()
        );
    }

    public WebhookDeliveryDetailResponse toDetail(
            WebhookDelivery delivery,
            List<WebhookDeliveryAttempt> attempts
    ) {
        return new WebhookDeliveryDetailResponse(
                toResponse(delivery),
                attempts.stream().map(this::toAttemptResponse).toList()
        );
    }

    private WebhookDeliveryAttemptResponse toAttemptResponse(
            WebhookDeliveryAttempt attempt
    ) {
        return new WebhookDeliveryAttemptResponse(
                attempt.getAttemptNumber(),
                attempt.getOutcome(),
                attempt.getRequestTimestamp(),
                attempt.getHttpStatus(),
                attempt.getDurationMs(),
                attempt.getResponseExcerpt(),
                attempt.getErrorCategory(),
                attempt.getErrorMessage(),
                attempt.getAttemptedAt()
        );
    }
}
