package com.crm.backend.publicapi;

import com.crm.backend.lead.LeadStatus;
import com.crm.backend.lead.dto.LeadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/leads")
public class PublicLeadController {

    private final PublicApiReadService readService;

    public PublicLeadController(PublicApiReadService readService) {
        this.readService = readService;
    }

    @GetMapping
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LeadStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(
                readService.getLeads(keyword, status, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getLead(@PathVariable Long id) {
        return ResponseEntity.ok(readService.getLead(id));
    }
}
