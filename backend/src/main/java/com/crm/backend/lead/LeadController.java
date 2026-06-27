package com.crm.backend.lead;

import com.crm.backend.lead.dto.CreateLeadRequest;
import com.crm.backend.lead.dto.LeadResponse;
import com.crm.backend.lead.dto.UpdateLeadRequest;
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
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PreAuthorize("hasAuthority('LEAD_CREATE')")

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(
            @Valid @RequestBody CreateLeadRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request, currentUser.getId()));
    }
    @PreAuthorize("hasAuthority('LEAD_VIEW')")

    @GetMapping
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LeadStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.getLeads(keyword, status, pageable));
    }
    @PreAuthorize("hasAuthority('LEAD_VIEW')")

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getLeadById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getLeadById(id));
    }
    @PreAuthorize("hasAuthority('LEAD_UPDATE')")

    @PutMapping("/{id}")
    public ResponseEntity<LeadResponse> updateLead(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeadRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(leadService.updateLead(id, request, currentUser.getId()));
    }

    @PreAuthorize("hasAuthority('LEAD_ARCHIVE')")

    @PatchMapping("/{id}/archive")
    public ResponseEntity<LeadResponse> archiveLead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(leadService.archiveLead(id, currentUser.getId()));
    }
    @PreAuthorize("hasAuthority('LEAD_CONVERT')")

    @PatchMapping("/{id}/convert")
    public ResponseEntity<LeadResponse> convertLead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(leadService.convertLead(id, currentUser.getId()));
    }
}
