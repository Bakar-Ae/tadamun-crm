package com.crm.backend.note.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        String content,
        Long customerId,
        String customerName,
        Long leadId,
        String leadName,
        Long createdByUserId,
        String createdByUserName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}