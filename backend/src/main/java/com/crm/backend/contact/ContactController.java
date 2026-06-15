package com.crm.backend.contact;

import com.crm.backend.contact.dto.ContactResponse;
import com.crm.backend.contact.dto.CreateContactRequest;
import com.crm.backend.contact.dto.UpdateContactRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contacts")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_REP', 'SUPPORT_STAFF')")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(@Valid @RequestBody CreateContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.createContact(request));
    }

    @GetMapping
    public ResponseEntity<Page<ContactResponse>> getContacts(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContactStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.getContacts(customerId, keyword, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContactRequest request
    ) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ContactResponse> archiveContact(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.archiveContact(id));
    }
}