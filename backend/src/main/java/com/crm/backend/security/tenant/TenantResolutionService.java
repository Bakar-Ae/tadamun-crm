package com.crm.backend.security.tenant;

import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.permission.Permission;
import com.crm.backend.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TenantResolutionService {

    private final OrganizationMembershipRepository membershipRepository;

    public TenantResolutionService(
            OrganizationMembershipRepository membershipRepository
    ) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public TenantContext resolve(Long userId, Long requestedOrganizationId) {
        if (userId == null) {
            throw TenantAccessException.accessDenied();
        }

        if (requestedOrganizationId != null) {
            return resolveRequestedOrganization(
                    userId,
                    requestedOrganizationId
            );
        }

        return resolveSingleActiveMembership(userId);
    }

    private TenantContext resolveRequestedOrganization(
            Long userId,
            Long organizationId
    ) {
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        organizationId,
                        userId,
                        OrganizationMembershipStatus.ACTIVE
                )
                .orElseThrow(TenantAccessException::accessDenied);

        ensureOrganizationActive(membership);

        return toContext(membership);
    }

    private TenantContext resolveSingleActiveMembership(Long userId) {
        List<OrganizationMembership> memberships = membershipRepository
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        userId,
                        OrganizationMembershipStatus.ACTIVE
                );

        List<OrganizationMembership> activeOrganizations = memberships.stream()
                .filter(membership ->
                        membership.getOrganization().getStatus()
                                == OrganizationStatus.ACTIVE
                )
                .toList();

        if (activeOrganizations.size() == 1) {
            return toContext(activeOrganizations.getFirst());
        }

        if (activeOrganizations.size() > 1) {
            throw TenantAccessException.contextRequired();
        }

        if (!memberships.isEmpty()) {
            throw TenantAccessException.organizationUnavailable();
        }

        throw TenantAccessException.accessDenied();
    }

    private void ensureOrganizationActive(
            OrganizationMembership membership
    ) {
        if (membership.getOrganization().getStatus()
                != OrganizationStatus.ACTIVE) {
            throw TenantAccessException.organizationUnavailable();
        }
    }

    private TenantContext toContext(
            OrganizationMembership membership
    ) {
        if (membership.getUser().getStatus() != UserStatus.ACTIVE) {
            throw TenantAccessException.accessDenied();
        }

        Set<com.crm.backend.permission.PermissionName> permissions =
                membership.getRole().getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toUnmodifiableSet());

        Long teamId = membership.getUser().getTeam() == null
                ? null
                : membership.getUser().getTeam().getId();

        return new TenantContext(
                membership.getOrganization().getId(),
                membership.getId(),
                membership.getUser().getId(),
                membership.getRole().getName(),
                membership.getRole().getDataScope(),
                teamId,
                permissions
        );
    }
}
