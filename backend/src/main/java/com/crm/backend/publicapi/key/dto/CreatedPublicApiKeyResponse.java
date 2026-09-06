package com.crm.backend.publicapi.key.dto;

public record CreatedPublicApiKeyResponse(
        String apiKey,
        PublicApiKeyResponse key
) {
}
