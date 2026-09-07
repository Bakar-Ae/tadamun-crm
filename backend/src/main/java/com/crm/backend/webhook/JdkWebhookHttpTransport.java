package com.crm.backend.webhook;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

@Component
public class JdkWebhookHttpTransport implements WebhookHttpTransport {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RESPONSE_BYTES = 4 * 1024;

    private final HttpClient httpClient;

    public JdkWebhookHttpTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    JdkWebhookHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public WebhookHttpResponse send(WebhookHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(request.endpoint())
                .timeout(RESPONSE_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(request.body()));
        request.headers().forEach(builder::header);

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            String excerpt;
            try (InputStream body = response.body()) {
                byte[] received = body.readNBytes(MAX_RESPONSE_BYTES + 1);
                excerpt = new String(
                        Arrays.copyOf(
                                received,
                                Math.min(received.length, MAX_RESPONSE_BYTES)
                        ),
                        StandardCharsets.UTF_8
                );
            }
            return new WebhookHttpResponse(
                    response.statusCode(),
                    excerpt,
                    response.headers().firstValue("Retry-After").orElse(null)
            );
        } catch (HttpTimeoutException exception) {
            throw transportFailure(
                    WebhookTransportErrorCategory.TIMEOUT,
                    "Webhook request timed out",
                    exception
            );
        } catch (ConnectException exception) {
            throw transportFailure(
                    WebhookTransportErrorCategory.CONNECTION_FAILED,
                    "Webhook connection failed",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw transportFailure(
                    WebhookTransportErrorCategory.INTERRUPTED,
                    "Webhook request was interrupted",
                    exception
            );
        } catch (IOException exception) {
            throw transportFailure(
                    WebhookTransportErrorCategory.IO_FAILURE,
                    "Webhook request failed",
                    exception
            );
        }
    }

    private WebhookTransportException transportFailure(
            WebhookTransportErrorCategory category,
            String message,
            Exception cause
    ) {
        return new WebhookTransportException(category, message, cause);
    }
}
