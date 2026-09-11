package com.crm.backend.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateIntegrationConnectionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Map<String, Object> configuration,
        Map<String, String> credentials
) {
}
