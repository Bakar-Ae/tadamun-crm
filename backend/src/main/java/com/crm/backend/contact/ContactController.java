package com.crm.backend.contact;

import com.crm.backend.contact.dto.ContactResponse;
import com.crm.backend.contact.dto.CreateContactRequest;
import com.crm.backend.contact.dto.UpdateContactRequest;
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
@RequestMapping("/api/v1/contacts")

public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }


    @PostMapping
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public ResponseEntity<ContactResponse> createContact(
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.createContact(request, currentUser.getId()));
    }
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")

    @GetMapping
    public ResponseEntity<Page<ContactResponse>> getContacts(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContactStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(contactService.getContacts(customerId, keyword, status, pageable));
    }
    @PreAuthorize("hasAuthority('CONTACT_VIEW')")

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContactRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(contactService.updateContact(id, request, currentUser.getId()));
    }

    @PreAuthorize("hasAuthority('CONTACT_ARCHIVE')")

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ContactResponse> archiveContact(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(contactService.archiveContact(id, currentUser.getId()));
    }
}
