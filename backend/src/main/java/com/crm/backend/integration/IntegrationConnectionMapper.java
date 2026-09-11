package com.crm.backend.integration;

import com.crm.backend.integration.dto.IntegrationConnectionResponse;
import com.crm.backend.integration.provider.IntegrationProviderRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class IntegrationConnectionMapper {

    private static final TypeReference<Map<String, Object>> CONFIG_MAP =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final IntegrationProviderRegistry providerRegistry;
    private final IntegrationCredentialService credentialService;

    public IntegrationConnectionMapper(
            ObjectMapper objectMapper,
            IntegrationProviderRegistry providerRegistry,
            IntegrationCredentialService credentialService
    ) {
        this.objectMapper = objectMapper;
        this.providerRegistry = providerRegistry;
        this.credentialService = credentialService;
    }

    public IntegrationConnectionResponse toResponse(
            IntegrationConnection connection
    ) {
        return new IntegrationConnectionResponse(
                connection.getId(),
                connection.getPublicConnectionId(),
                connection.getProvider(),
                connection.getName(),
                connection.getStatus(),
                configuration(connection.getConfiguration()),
                providerRegistry.require(
                        connection.getProvider()
                ).capabilities(),
                credentialService.isConfigured(connection),
                connection.getExternalAccountId(),
                connection.getCredentialsUpdatedAt(),
                connection.getLastVerifiedAt(),
                connection.getLastErrorCategory(),
                connection.getLastErrorMessage(),
                connection.getCreatedByUser().getId(),
                connection.getCreatedByUser().getFullName(),
                connection.getRevokedAt(),
                connection.getCreatedAt(),
                connection.getUpdatedAt()
        );
    }

    private Map<String, Object> configuration(String value) {
        try {
            return objectMapper.readValue(value, CONFIG_MAP);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Stored integration configuration is invalid",
                    exception
            );
        }
    }
}
