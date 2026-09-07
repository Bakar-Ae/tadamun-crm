package com.crm.backend.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookDeliveryServiceTest {

    private WebhookDeliveryPreparationService preparationService;
    private WebhookRequestSigner requestSigner;
    private WebhookHttpTransport transport;
    private WebhookDeliveryResultService resultService;
    private WebhookDeliveryService service;

    @BeforeEach
    void setUp() {
        preparationService = mock(WebhookDeliveryPreparationService.class);
        requestSigner = mock(WebhookRequestSigner.class);
        transport = mock(WebhookHttpTransport.class);
        resultService = mock(WebhookDeliveryResultService.class);
        service = new WebhookDeliveryService(
                preparationService,
                requestSigner,
                transport,
                resultService
        );
        when(preparationService.prepare(20L)).thenReturn(prepared());
        when(requestSigner.signatureHeader(any(), any(Long.class), any()))
                .thenReturn("t=1700000000,v1=signature");
    }

    @Test
    void shouldSendRequiredHeadersAndRecordSuccess() {
        when(transport.send(any())).thenReturn(
                new WebhookHttpResponse(204, "", null)
        );
        when(resultService.record(any(), any(Integer.class), any()))
                .thenReturn(WebhookDeliveryOutcome.SUCCEEDED);

        WebhookDeliveryOutcome outcome = service.deliver(20L);

        assertEquals(WebhookDeliveryOutcome.SUCCEEDED, outcome);
        ArgumentCaptor<WebhookHttpRequest> requestCaptor =
                ArgumentCaptor.forClass(WebhookHttpRequest.class);
        verify(transport).send(requestCaptor.capture());
        WebhookHttpRequest request = requestCaptor.getValue();
        assertEquals("application/json", request.headers().get(
                "Content-Type"
        ));
        assertEquals("Tadamun-Webhooks/1.0", request.headers().get(
                "User-Agent"
        ));
        assertEquals("evt_fixed", request.headers().get(
                "X-Tadamun-Event-Id"
        ));
        assertEquals("dlv_fixed", request.headers().get(
                "X-Tadamun-Delivery-Id"
        ));
        assertEquals("t=1700000000,v1=signature", request.headers().get(
                "X-Tadamun-Signature"
        ));
    }

    @Test
    void shouldClassifyRateLimitAsRetryable() {
        when(transport.send(any())).thenReturn(
                new WebhookHttpResponse(429, "slow down", "30")
        );
        when(resultService.record(any(), any(Integer.class), any()))
                .thenReturn(WebhookDeliveryOutcome.RETRYABLE_FAILURE);

        service.deliver(20L);

        ArgumentCaptor<WebhookDeliveryResult> resultCaptor =
                ArgumentCaptor.forClass(WebhookDeliveryResult.class);
        verify(resultService).record(
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(1),
                resultCaptor.capture()
        );
        assertEquals(WebhookDeliveryOutcome.RETRYABLE_FAILURE,
                resultCaptor.getValue().outcome());
        assertEquals(429, resultCaptor.getValue().httpStatus());
        assertEquals("30", resultCaptor.getValue().retryAfter());
    }

    @Test
    void shouldConvertSafeTransportFailureToRetryableResult() {
        when(transport.send(any())).thenThrow(new WebhookTransportException(
                WebhookTransportErrorCategory.TIMEOUT,
                "Webhook request timed out",
                new RuntimeException("private network detail")
        ));
        when(resultService.record(any(), any(Integer.class), any()))
                .thenReturn(WebhookDeliveryOutcome.RETRYABLE_FAILURE);

        service.deliver(20L);

        ArgumentCaptor<WebhookDeliveryResult> resultCaptor =
                ArgumentCaptor.forClass(WebhookDeliveryResult.class);
        verify(resultService).record(
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(1),
                resultCaptor.capture()
        );
        assertEquals("TIMEOUT", resultCaptor.getValue().errorCategory());
        assertEquals("Webhook request timed out",
                resultCaptor.getValue().errorMessage());
    }

    private PreparedWebhookDelivery prepared() {
        return new PreparedWebhookDelivery(
                20L,
                1,
                URI.create("https://hooks.example.com/events"),
                "dlv_fixed",
                "evt_fixed",
                "customer.created",
                "{\"id\":\"evt_fixed\"}".getBytes(StandardCharsets.UTF_8),
                List.of("whsec_current"),
                1_700_000_000L
        );
    }
}
