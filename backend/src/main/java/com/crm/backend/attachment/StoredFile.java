package com.crm.backend.attachment;

public record StoredFile(
        String storageKey,
        String contentType,
        long sizeBytes,
        String checksumSha256
) {
}