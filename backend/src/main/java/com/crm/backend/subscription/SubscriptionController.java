package com.crm.backend.subscription;

import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.subscription.billing.SubscriptionBillingService;
import com.crm.backend.subscription.billing.dto.BillingSessionResponse;
import com.crm.backend.subscription.billing.dto.CreateBillingCheckoutRequest;
import com.crm.backend.subscription.dto.OrganizationSubscriptionResponse;
import com.crm.backend.subscription.dto.SubscriptionPlanResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionBillingService billingService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            SubscriptionBillingService billingService
    ) {
        this.subscriptionService = subscriptionService;
        this.billingService = billingService;
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

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_MANAGE')")
    public ResponseEntity<BillingSessionResponse> createCheckout(
            @Valid @RequestBody CreateBillingCheckoutRequest request,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(billingService.createCheckout(
                request,
                currentUser.getUsername(),
                currentUser.getId(),
                idempotencyKey
        ));
    }

    @PostMapping("/portal")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_MANAGE')")
    public ResponseEntity<BillingSessionResponse> createPortal(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                billingService.createPortal(currentUser.getId())
        );
    }
}
