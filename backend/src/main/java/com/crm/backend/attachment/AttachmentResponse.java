package com.crm.backend.attachment;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        Long customerId,
        Long leadId,
        Long uploadedByUserId,
        String uploadedByName,
        AttachmentStatus status,
        LocalDateTime createdAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getChecksumSha256(),
                attachment.getCustomer() == null
                        ? null
                        : attachment.getCustomer().getId(),
                attachment.getLead() == null
                        ? null
                        : attachment.getLead().getId(),
                attachment.getUploadedByUser().getId(),
                attachment.getUploadedByUser().getFullName(),
                attachment.getStatus(),
                attachment.getCreatedAt()
        );
    }
}