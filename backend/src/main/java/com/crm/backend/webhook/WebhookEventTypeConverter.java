package com.crm.backend.webhook;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WebhookEventTypeConverter
        implements AttributeConverter<WebhookEventType, String> {

    @Override
    public String convertToDatabaseColumn(WebhookEventType eventType) {
        return eventType == null ? null : eventType.getValue();
    }

    @Override
    public WebhookEventType convertToEntityAttribute(String value) {
        return value == null ? null : WebhookEventType.fromValue(value);
    }
}