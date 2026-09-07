package com.crm.backend.webhook;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WebhookDeliveryService {

    private static final String USER_AGENT = "Tadamun-Webhooks/1.0";

    private final WebhookDeliveryPreparationService preparationService;
    private final WebhookRequestSigner requestSigner;
    private final WebhookHttpTransport httpTransport;
    private final WebhookDeliveryResultService resultService;

    public WebhookDeliveryService(
            WebhookDeliveryPreparationService preparationService,
            WebhookRequestSigner requestSigner,
            WebhookHttpTransport httpTransport,
            WebhookDeliveryResultService resultService
    ) {
        this.preparationService = preparationService;
        this.requestSigner = requestSigner;
        this.httpTransport = httpTransport;
        this.resultService = resultService;
    }

    public WebhookDeliveryOutcome deliver(Long deliveryId) {
        return deliver(deliveryId, null);
    }

    public WebhookDeliveryOutcome deliver(
            Long deliveryId,
            String expectedClaimToken
    ) {
        PreparedWebhookDelivery prepared = expectedClaimToken == null
                ? preparationService.prepare(deliveryId)
                : preparationService.prepare(deliveryId, expectedClaimToken);
        byte[] body = prepared.body();
        String signature = requestSigner.signatureHeader(
                prepared.signingSecrets(),
                prepared.requestTimestamp(),
                body
        );
        WebhookHttpRequest request = new WebhookHttpRequest(
                prepared.endpoint(),
                body,
                requestHeaders(prepared, signature)
        );

        long startedAt = System.nanoTime();
        WebhookDeliveryResult result;
        try {
            WebhookHttpResponse response = httpTransport.send(request);
            result = classify(
                    response,
                    elapsedMilliseconds(startedAt),
                    prepared.requestTimestamp()
            );
        } catch (WebhookTransportException exception) {
            result = new WebhookDeliveryResult(
                    WebhookDeliveryOutcome.RETRYABLE_FAILURE,
                    null,
                    elapsedMilliseconds(startedAt),
                    null,
                    exception.getCategory().name(),
                    exception.getMessage(),
                    prepared.requestTimestamp(),
                    null
            );
        }

        return resultService.record(
                prepared.deliveryId(),
                prepared.attemptNumber(),
                result
        );
    }

    private Map<String, String> requestHeaders(
            PreparedWebhookDelivery prepared,
            String signature
    ) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", USER_AGENT);
        headers.put("X-Tadamun-Event-Id", prepared.publicEventId());
        headers.put("X-Tadamun-Event-Type", prepared.eventType());
        headers.put("X-Tadamun-Delivery-Id", prepared.publicDeliveryId());
        headers.put("X-Tadamun-Signature", signature);
        return headers;
    }

    private WebhookDeliveryResult classify(
            WebhookHttpResponse response,
            int durationMs,
            long requestTimestamp
    ) {
        int statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return new WebhookDeliveryResult(
                    WebhookDeliveryOutcome.SUCCEEDED,
                    statusCode,
                    durationMs,
                    response.responseExcerpt(),
                    null,
                    null,
                    requestTimestamp,
                    null
            );
        }

        boolean retryable = statusCode == 408
                || statusCode == 425
                || statusCode == 429
                || statusCode >= 500;
        return new WebhookDeliveryResult(
                retryable
                        ? WebhookDeliveryOutcome.RETRYABLE_FAILURE
                        : WebhookDeliveryOutcome.TERMINAL_FAILURE,
                statusCode,
                durationMs,
                response.responseExcerpt(),
                retryable ? "HTTP_RETRYABLE" : "HTTP_TERMINAL",
                "Webhook endpoint returned HTTP " + statusCode,
                requestTimestamp,
                response.retryAfter()
        );
    }

    private int elapsedMilliseconds(long startedAt) {
        long elapsed = Math.max(0L, System.nanoTime() - startedAt);
        return (int) Math.min(
                Integer.MAX_VALUE,
                elapsed / 1_000_000L
        );
    }
}
