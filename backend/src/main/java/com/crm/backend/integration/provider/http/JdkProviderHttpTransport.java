package com.crm.backend.integration.provider.http;

import com.crm.backend.integration.provider.IntegrationProviderException;
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
public class JdkProviderHttpTransport implements ProviderHttpTransport {

    private static final int MAX_RESPONSE_BYTES = 16 * 1024;
    private final HttpClient httpClient;

    public JdkProviderHttpTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    JdkProviderHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ProviderHttpResponse send(ProviderHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(request.uri())
                .timeout(request.timeout());
        request.headers().forEach(builder::header);
        if ("GET".equals(request.method())) {
            builder.GET();
        } else if ("POST".equals(request.method())) {
            builder.POST(HttpRequest.BodyPublishers.ofString(
                    request.body() == null ? "" : request.body(),
                    StandardCharsets.UTF_8
            ));
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method");
        }

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            try (InputStream body = response.body()) {
                byte[] received = body.readNBytes(MAX_RESPONSE_BYTES + 1);
                return new ProviderHttpResponse(
                        response.statusCode(),
                        new String(
                                Arrays.copyOf(
                                        received,
                                        Math.min(received.length,
                                                MAX_RESPONSE_BYTES)
                                ),
                                StandardCharsets.UTF_8
                        )
                );
            }
        } catch (HttpTimeoutException exception) {
            throw failure("TIMEOUT", "Provider request timed out", exception);
        } catch (ConnectException exception) {
            throw failure(
                    "CONNECTION_FAILED",
                    "Provider connection failed",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure(
                    "INTERRUPTED",
                    "Provider request was interrupted",
                    exception
            );
        } catch (IOException exception) {
            throw failure("IO_FAILURE", "Provider request failed", exception);
        }
    }

    private IntegrationProviderException failure(
            String category,
            String message,
            Exception cause
    ) {
        return new IntegrationProviderException(
                category,
                message,
                true,
                cause
        );
    }
}
