package com.crm.backend.publicapi.key;

public record GeneratedPublicApiKey(
        String rawKey,
        String publicId,
        String displayPrefix,
        String secretHash
) {
}
