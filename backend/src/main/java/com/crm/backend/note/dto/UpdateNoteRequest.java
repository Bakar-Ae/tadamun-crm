package com.crm.backend.note.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateNoteRequest(
        @NotBlank(message = "Content is required")
        String content
) {
}