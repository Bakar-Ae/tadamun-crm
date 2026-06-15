package com.crm.backend.note;

import com.crm.backend.note.dto.CreateNoteRequest;
import com.crm.backend.note.dto.NoteResponse;
import com.crm.backend.note.dto.UpdateNoteRequest;
import com.crm.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notes")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_REP', 'SUPPORT_STAFF')")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.createNote(request, currentUser.getId()));
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<Page<NoteResponse>> getCustomerNotes(
            @PathVariable Long customerId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(noteService.getCustomerNotes(customerId, pageable));
    }

    @GetMapping("/leads/{leadId}")
    public ResponseEntity<Page<NoteResponse>> getLeadNotes(
            @PathVariable Long leadId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(noteService.getLeadNotes(leadId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        return ResponseEntity.ok(noteService.updateNote(id, request));
    }
}