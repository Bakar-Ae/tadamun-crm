package com.crm.backend.note;

import com.crm.backend.note.dto.NoteResponse;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getContent(),
                note.getCustomer() == null ? null : note.getCustomer().getId(),
                note.getCustomer() == null ? null : note.getCustomer().getName(),
                note.getLead() == null ? null : note.getLead().getId(),
                note.getLead() == null ? null : note.getLead().getFullName(),
                note.getCreatedByUser().getId(),
                note.getCreatedByUser().getFullName(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}