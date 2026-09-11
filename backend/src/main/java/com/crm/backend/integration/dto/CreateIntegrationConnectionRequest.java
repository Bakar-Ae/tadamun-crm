package com.crm.backend.integration.dto;

import com.crm.backend.integration.IntegrationProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateIntegrationConnectionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull IntegrationProvider provider,
        @NotNull Map<String, Object> configuration,
        @NotNull Map<String, String> credentials
) {
}
