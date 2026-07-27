package com.crm.backend.organization;

import com.crm.backend.organization.dto.OrganizationResponse;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public OrganizationResponse toResponse(Organization organization) {
        var createdBy = organization.getCreatedByUser();

        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getTimeZone(),
                createdBy == null ? null : createdBy.getId(),
                createdBy == null ? null : createdBy.getFullName(),
                organization.getVersion(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}