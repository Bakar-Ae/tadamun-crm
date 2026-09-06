package com.crm.backend.publicapi.key;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PublicApiScopeConverter
        implements AttributeConverter<PublicApiScope, String> {

    @Override
    public String convertToDatabaseColumn(PublicApiScope scope) {
        return scope == null ? null : scope.getValue();
    }

    @Override
    public PublicApiScope convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return PublicApiScope.fromValue(value);
    }
}
