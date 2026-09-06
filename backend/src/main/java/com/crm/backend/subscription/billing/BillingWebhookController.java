package com.crm.backend.subscription.billing;

import com.crm.backend.subscription.billing.dto.BillingWebhookResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/webhooks")
public class BillingWebhookController {

    private final BillingWebhookService webhookService;

    public BillingWebhookController(BillingWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping(
            value = "/stripe",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BillingWebhookResponse> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        BillingWebhookProcessingResult result =
                webhookService.processStripeWebhook(payload, signature);
        BillingWebhookResponse response = new BillingWebhookResponse(
                result.received(),
                result.duplicate(),
                result.status()
        );

        if (result.status() == BillingWebhookProcessingStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }
}
