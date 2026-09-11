package com.crm.backend.integration.provider.whatsapp;

import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.provider.IntegrationCapability;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
import com.crm.backend.integration.provider.IntegrationOutboundMessage;
import com.crm.backend.integration.provider.IntegrationProviderAdapter;
import com.crm.backend.integration.provider.IntegrationProviderContext;
import com.crm.backend.integration.provider.IntegrationProviderDeliveryResult;
import com.crm.backend.integration.provider.IntegrationProviderException;
import com.crm.backend.integration.provider.IntegrationSecrets;
import com.crm.backend.integration.provider.IntegrationVerificationResult;
import com.crm.backend.integration.provider.http.ProviderHttpRequest;
import com.crm.backend.integration.provider.http.ProviderHttpResponse;
import com.crm.backend.integration.provider.http.ProviderHttpTransport;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class WhatsAppCloudAdapter implements IntegrationProviderAdapter {

    private static final Pattern PHONE_NUMBER_ID = Pattern.compile("\\d{5,30}");
    private static final Pattern API_VERSION = Pattern.compile("v\\d+\\.\\d+");
    private static final Pattern DESTINATION = Pattern.compile(
            "\\+?[1-9]\\d{6,19}"
    );

    private final ProviderHttpTransport transport;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration responseTimeout;

    public WhatsAppCloudAdapter(
            ProviderHttpTransport transport,
            ObjectMapper objectMapper,
            WhatsAppCloudProperties properties
    ) {
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.baseUrl = normalizeBaseUrl(properties.baseUrl());
        this.responseTimeout = Duration.ofSeconds(
                Math.max(1, properties.responseTimeoutSeconds())
        );
    }

    @Override
    public IntegrationProvider provider() {
        return IntegrationProvider.WHATSAPP_CLOUD;
    }

    @Override
    public Set<IntegrationCapability> capabilities() {
        return Set.of(
                IntegrationCapability.SEND_MESSAGE,
                IntegrationCapability.TEST_CONNECTION
        );
    }

    @Override
    public void validateConfiguration(Map<String, Object> configuration) {
        requirePattern(
                configuration,
                "phoneNumberId",
                PHONE_NUMBER_ID,
                "WhatsApp phone number ID is invalid"
        );
        requirePattern(
                configuration,
                "apiVersion",
                API_VERSION,
                "WhatsApp Graph API version is invalid"
        );
    }

    @Override
    public void validateCredentials(IntegrationSecrets secrets) {
        if (secrets.require("accessToken").length() > 4096) {
            throw new IllegalArgumentException(
                    "WhatsApp access token is too long"
            );
        }
    }

    @Override
    public IntegrationVerificationResult verify(
            IntegrationProviderContext context
    ) {
        validateConfiguration(context.configuration());
        validateCredentials(context.secrets());
        String phoneNumberId = text(
                context.configuration(),
                "phoneNumberId"
        );
        URI uri = URI.create(endpoint(context.configuration(), phoneNumberId)
                + "?fields=id,display_phone_number,verified_name");
        ProviderHttpResponse response = transport.send(new ProviderHttpRequest(
                "GET",
                uri,
                headers(context.secrets()),
                null,
                responseTimeout
        ));
        requireSuccess(response, "verify");
        return IntegrationVerificationResult.success(
                responseId(response.body(), phoneNumberId)
        );
    }

    @Override
    public IntegrationProviderDeliveryResult deliver(
            IntegrationProviderContext context,
            IntegrationOutboundMessage message
    ) {
        if (message.type() != IntegrationDeliveryType.WHATSAPP_TEXT) {
            throw new IllegalArgumentException(
                    "WhatsApp connections only support text messages"
            );
        }
        validateConfiguration(context.configuration());
        validateCredentials(context.secrets());
        if (!DESTINATION.matcher(message.destination()).matches()) {
            throw new IllegalArgumentException(
                    "WhatsApp destination must be an international number"
            );
        }
        if (message.body().isBlank() || message.body().length() > 4096) {
            throw new IllegalArgumentException(
                    "WhatsApp message must contain 1 to 4096 characters"
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", message.destination().replace("+", ""));
        payload.put("type", "text");
        payload.put("text", Map.of(
                "preview_url", false,
                "body", message.body()
        ));
        ProviderHttpResponse response = transport.send(new ProviderHttpRequest(
                "POST",
                URI.create(endpoint(
                        context.configuration(),
                        text(context.configuration(), "phoneNumberId")
                ) + "/messages"),
                headers(context.secrets()),
                writeJson(payload),
                responseTimeout
        ));
        requireSuccess(response, "send message");
        return new IntegrationProviderDeliveryResult(
                messageId(response.body())
        );
    }

    private Map<String, String> headers(IntegrationSecrets secrets) {
        return Map.of(
                "Authorization", "Bearer " + secrets.require("accessToken"),
                "Content-Type", "application/json"
        );
    }

    private String endpoint(
            Map<String, Object> configuration,
            String resource
    ) {
        return baseUrl + '/' + text(configuration, "apiVersion") + '/'
                + resource;
    }

    private String responseId(String body, String fallback) {
        try {
            JsonNode id = objectMapper.readTree(body).get("id");
            return id == null || id.asText().isBlank()
                    ? fallback
                    : id.asText();
        } catch (JacksonException exception) {
            return fallback;
        }
    }

    private String messageId(String body) {
        try {
            JsonNode messages = objectMapper.readTree(body).get("messages");
            if (messages != null && messages.isArray() && !messages.isEmpty()) {
                JsonNode id = messages.get(0).get("id");
                if (id != null && !id.asText().isBlank()) {
                    return id.asText();
                }
            }
            return null;
        } catch (JacksonException exception) {
            return null;
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not prepare WhatsApp request",
                    exception
            );
        }
    }

    private void requireSuccess(
            ProviderHttpResponse response,
            String operation
    ) {
        if (response.successful()) {
            return;
        }
        boolean retryable = response.statusCode() == 408
                || response.statusCode() == 429
                || response.statusCode() >= 500;
        String category = "HTTP_" + response.statusCode();
        String message = response.statusCode() == 401
                || response.statusCode() == 403
                ? "WhatsApp rejected the configured credentials"
                : "WhatsApp could not " + operation;
        throw new IntegrationProviderException(
                category,
                message,
                retryable
        );
    }

    private void requirePattern(
            Map<String, Object> configuration,
            String field,
            Pattern pattern,
            String message
    ) {
        if (!pattern.matcher(text(configuration, field)).matches()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String text(Map<String, Object> configuration, String field) {
        Object value = configuration.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Integration configuration requires " + field
            );
        }
        return text.trim();
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp Cloud API base URL is required"
            );
        }
        URI uri = URI.create(value.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "WhatsApp Cloud API base URL must be HTTPS"
            );
        }
        String normalized = uri.toString();
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }
}
