package com.crm.backend.organization.workspace;

import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.organization.workspace.dto.WorkspaceResponse;
import com.crm.backend.security.tenant.TenantPermissionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class WorkspaceService {

    private final OrganizationMembershipRepository membershipRepository;
    private final TenantPermissionPolicy tenantPermissionPolicy;

    public WorkspaceService(
            OrganizationMembershipRepository membershipRepository,
            TenantPermissionPolicy tenantPermissionPolicy
    ) {
        this.membershipRepository = membershipRepository;
        this.tenantPermissionPolicy = tenantPermissionPolicy;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getActiveWorkspaces(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }

        return membershipRepository
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        userId,
                        OrganizationMembershipStatus.ACTIVE
                )
                .stream()
                .filter(membership ->
                        membership.getOrganization().getStatus()
                                == OrganizationStatus.ACTIVE
                )
                .map(membership -> new WorkspaceResponse(
                        membership.getOrganization().getId(),
                        membership.getId(),
                        membership.getOrganization().getName(),
                        membership.getOrganization().getSlug(),
                        membership.getOrganization().getTimeZone(),
                        membership.getRole().getName(),
                        membership.getRole().getDataScope(),
                        tenantPermissionPolicy
                                .resolvePermissions(membership.getRole())
                                .stream()
                                .sorted(Comparator.comparing(Enum::name))
                                .toList()
                ))
                .toList();
    }
}
