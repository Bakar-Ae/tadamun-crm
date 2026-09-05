package com.crm.backend.subscription;

import com.crm.backend.subscription.dto.OrganizationSubscriptionResponse;
import com.crm.backend.subscription.dto.SubscriptionPlanResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VIEW')")
    public ResponseEntity<OrganizationSubscriptionResponse>
    getCurrentSubscription() {
        return ResponseEntity.ok(
                subscriptionService.getCurrentSubscription()
        );
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VIEW')")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }
}
