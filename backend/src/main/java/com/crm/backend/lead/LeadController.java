package com.crm.backend.lead;

import com.crm.backend.lead.dto.CreateLeadRequest;
import com.crm.backend.lead.dto.LeadResponse;
import com.crm.backend.lead.dto.UpdateLeadRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leads")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_REP')")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody CreateLeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request));
    }

    @GetMapping
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LeadStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.getLeads(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getLeadById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getLeadById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadResponse> updateLead(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeadRequest request
    ) {
        return ResponseEntity.ok(leadService.updateLead(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<LeadResponse> archiveLead(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.archiveLead(id));
    }

    @PatchMapping("/{id}/convert")
    public ResponseEntity<LeadResponse> convertLead(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.convertLead(id));
    }
}
