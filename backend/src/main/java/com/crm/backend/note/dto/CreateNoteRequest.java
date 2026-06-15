package com.crm.backend.note.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(
        @NotBlank(message = "Content is required")
        String content,

        Long customerId,

        Long leadId
) {
}