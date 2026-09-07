package com.crm.backend.webhook;

public class WebhookTransportException extends RuntimeException {

    private final WebhookTransportErrorCategory category;

    public WebhookTransportException(
            WebhookTransportErrorCategory category,
            String safeMessage,
            Throwable cause
    ) {
        super(safeMessage, cause);
        this.category = category;
    }

    public WebhookTransportErrorCategory getCategory() {
        return category;
    }
}
