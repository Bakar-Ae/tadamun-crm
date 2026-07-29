package com.crm.backend.organization.membership;

import com.crm.backend.organization.membership.dto.OrganizationMembershipResponse;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMembershipMapper {

    public OrganizationMembershipResponse toResponse(
            OrganizationMembership membership
    ) {
        var organization = membership.getOrganization();
        var user = membership.getUser();
        var role = membership.getRole();

        return new OrganizationMembershipResponse(
                membership.getId(),
                organization == null ? null : organization.getId(),
                organization == null ? null : organization.getName(),
                organization == null ? null : organization.getSlug(),
                organization == null ? null : organization.getStatus(),
                user == null ? null : user.getId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getStatus(),
                role == null ? null : role.getName(),
                membership.getStatus(),
                membership.getJoinedAt(),
                membership.getVersion(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }
}