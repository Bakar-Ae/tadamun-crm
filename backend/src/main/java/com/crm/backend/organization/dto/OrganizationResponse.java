package com.crm.backend.organization.dto;

import com.crm.backend.organization.OrganizationStatus;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String slug,
        OrganizationStatus status,
        String timeZone,
        Long createdByUserId,
        String createdByName,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}