package com.crm.backend.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<Page<ActivityEventResponse>> getCustomerActivity(
            @PathVariable Long customerId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                activityService.getCustomerActivity(customerId, pageable)
        );
    }

    @GetMapping("/leads/{leadId}")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<Page<ActivityEventResponse>> getLeadActivity(
            @PathVariable Long leadId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                activityService.getLeadActivity(leadId, pageable)
        );
    }
}