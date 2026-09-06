package com.crm.backend.publicapi.key;

import com.crm.backend.publicapi.key.dto.PublicApiKeyResponse;
import com.crm.backend.user.User;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Component
public class PublicApiKeyMapper {

    public PublicApiKeyResponse toResponse(PublicApiKey apiKey) {
        User createdBy = apiKey.getCreatedByUser();
        User revokedBy = apiKey.getRevokedByUser();

        return new PublicApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getDisplayPrefix(),
                apiKey.getStatus(),
                apiKey.getScopes().stream()
                        .map(PublicApiScope::getValue)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                apiKey.getRateLimitPerMinute(),
                apiKey.getExpiresAt(),
                apiKey.getLastUsedAt(),
                createdBy.getId(),
                createdBy.getFullName(),
                revokedBy == null ? null : revokedBy.getId(),
                revokedBy == null ? null : revokedBy.getFullName(),
                apiKey.getRevokedAt(),
                apiKey.getCreatedAt()
        );
    }
}
