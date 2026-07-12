package com.crm.backend.report.export;

import java.util.Objects;

public record ReportExportResult(
        String fileName,
        String contentType,
        byte[] content
) {
    public ReportExportResult {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Export file name is required");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Export content type is required");
        }

        content = Objects.requireNonNull(
                content,
                "Export content is required"
        ).clone();

        if (content.length == 0) {
            throw new IllegalArgumentException("Export content cannot be empty");
        }
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}