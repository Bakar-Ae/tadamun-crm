package com.crm.backend.security.tenant;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentOrganizationProvider {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public CurrentOrganizationProvider(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
    }

    public Long getOrganizationId() {
        return TenantContextHolder.getRequired().organizationId();
    }

    public Organization getOrganizationReference() {
        return organizationRepository.getReferenceById(
                getOrganizationId()
        );
    }

    public Optional<Organization> getOptionalOrganizationReference() {
        return TenantContextHolder.getOptional()
                .map(context -> organizationRepository.getReferenceById(
                        context.organizationId()
                ));
    }

    public void requireActiveUserMembership(Long userId) {
        if (!membershipRepository
                .existsByOrganizationIdAndUserIdAndStatus(
                        getOrganizationId(),
                        userId,
                        OrganizationMembershipStatus.ACTIVE
                )) {
            throw new ResourceNotFoundException("User not found");
        }
    }
}
