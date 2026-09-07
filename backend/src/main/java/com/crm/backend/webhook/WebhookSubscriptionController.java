package com.crm.backend.webhook;

import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.webhook.dto.CreateWebhookSubscriptionRequest;
import com.crm.backend.webhook.dto.UpdateWebhookSubscriptionRequest;
import com.crm.backend.webhook.dto.WebhookDeliveryDetailResponse;
import com.crm.backend.webhook.dto.WebhookDeliveryResponse;
import com.crm.backend.webhook.dto.WebhookSubscriptionResponse;
import com.crm.backend.webhook.dto.WebhookSubscriptionSecretResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/webhook-subscriptions")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService subscriptionService;
    private final WebhookDeliveryHistoryService deliveryHistoryService;

    public WebhookSubscriptionController(
            WebhookSubscriptionService subscriptionService,
            WebhookDeliveryHistoryService deliveryHistoryService
    ) {
        this.subscriptionService = subscriptionService;
        this.deliveryHistoryService = deliveryHistoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WEBHOOK_VIEW')")
    public ResponseEntity<Page<WebhookSubscriptionResponse>>
    getSubscriptions(
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                subscriptionService.getSubscriptions(pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WEBHOOK_VIEW')")
    public ResponseEntity<WebhookSubscriptionResponse> getSubscription(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                subscriptionService.getSubscription(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookSubscriptionSecretResponse>
    createSubscription(
            @Valid @RequestBody CreateWebhookSubscriptionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                subscriptionService.createSubscription(
                        request,
                        userDetails.getId()
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookSubscriptionResponse> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWebhookSubscriptionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                subscriptionService.updateSubscription(
                        id,
                        request,
                        userDetails.getId()
                )
        );
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookSubscriptionResponse> enableSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                subscriptionService.enableSubscription(
                        id,
                        userDetails.getId()
                )
        );
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookSubscriptionResponse> disableSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                subscriptionService.disableSubscription(
                        id,
                        userDetails.getId()
                )
        );
    }

    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookSubscriptionSecretResponse> rotateSecret(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                subscriptionService.rotateSecret(
                        id,
                        userDetails.getId()
                )
        );
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookSubscriptionResponse> revokeSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                subscriptionService.revokeSubscription(
                        id,
                        userDetails.getId()
                )
        );
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasAuthority('WEBHOOK_VIEW')")
    public ResponseEntity<Page<WebhookDeliveryResponse>> getDeliveries(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                deliveryHistoryService.getDeliveries(id, pageable)
        );
    }

    @GetMapping("/{id}/deliveries/{deliveryId}")
    @PreAuthorize("hasAuthority('WEBHOOK_VIEW')")
    public ResponseEntity<WebhookDeliveryDetailResponse> getDelivery(
            @PathVariable Long id,
            @PathVariable String deliveryId
    ) {
        return ResponseEntity.ok(
                deliveryHistoryService.getDelivery(id, deliveryId)
        );
    }

    @PostMapping("/{id}/deliveries/{deliveryId}/replay")
    @PreAuthorize("hasAuthority('WEBHOOK_MANAGE')")
    public ResponseEntity<WebhookDeliveryResponse> replayDelivery(
            @PathVariable Long id,
            @PathVariable String deliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(deliveryHistoryService.replay(
                id,
                deliveryId,
                userDetails.getId()
        ));
    }
}
