package com.crm.backend.webhook;

public record WebhookDeliveryResult(
        WebhookDeliveryOutcome outcome,
        Integer httpStatus,
        int durationMs,
        String responseExcerpt,
        String errorCategory,
        String errorMessage,
        long requestTimestamp,
        String retryAfter
) {
    public WebhookDeliveryResult(
            WebhookDeliveryOutcome outcome,
            Integer httpStatus,
            int durationMs,
            String responseExcerpt,
            String errorCategory,
            String errorMessage,
            long requestTimestamp
    ) {
        this(
                outcome,
                httpStatus,
                durationMs,
                responseExcerpt,
                errorCategory,
                errorMessage,
                requestTimestamp,
                null
        );
    }
}
