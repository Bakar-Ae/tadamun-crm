package com.crm.backend.publicapi.key.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public record CreatePublicApiKeyRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotEmpty
        Set<@NotBlank String> scopes,

        @Min(1)
        @Max(10000)
        Integer rateLimitPerMinute,

        @Future
        LocalDateTime expiresAt
) {
}
