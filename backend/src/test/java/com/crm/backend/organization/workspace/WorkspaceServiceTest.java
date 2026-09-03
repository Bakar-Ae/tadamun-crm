package com.crm.backend.organization.workspace;

import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.permission.Permission;
import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantPermissionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {

    @Test
    void getActiveWorkspacesReturnsActiveOrganizationsAndTenantPermissions() {
        OrganizationMembershipRepository membershipRepository =
                mock(OrganizationMembershipRepository.class);
        WorkspaceService workspaceService = new WorkspaceService(
                membershipRepository,
                new TenantPermissionPolicy()
        );

        OrganizationMembership activeMembership = membership(
                20L,
                organization(10L, "Tadamun", OrganizationStatus.ACTIVE),
                role()
        );
        OrganizationMembership inactiveOrganizationMembership = membership(
                21L,
                organization(11L, "Closed workspace", OrganizationStatus.ARCHIVED),
                role()
        );

        when(membershipRepository
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        1L,
                        OrganizationMembershipStatus.ACTIVE
                )).thenReturn(List.of(
                        activeMembership,
                        inactiveOrganizationMembership
                ));

        var workspaces = workspaceService.getActiveWorkspaces(1L);

        assertEquals(1, workspaces.size());
        assertEquals(10L, workspaces.getFirst().organizationId());
        assertEquals(20L, workspaces.getFirst().membershipId());
        assertEquals("Tadamun", workspaces.getFirst().name());
        assertEquals(RoleName.ADMIN, workspaces.getFirst().role());
        assertEquals(
                List.of(
                        PermissionName.CUSTOMER_VIEW,
                        PermissionName.DASHBOARD_VIEW
                ),
                workspaces.getFirst().permissions()
        );
        verify(membershipRepository)
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        1L,
                        OrganizationMembershipStatus.ACTIVE
                );
    }

    private OrganizationMembership membership(
            Long id,
            Organization organization,
            Role role
    ) {
        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(id);
        membership.setOrganization(organization);
        membership.setRole(role);
        membership.setStatus(OrganizationMembershipStatus.ACTIVE);
        return membership;
    }

    private Organization organization(
            Long id,
            String name,
            OrganizationStatus status
    ) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName(name);
        organization.setSlug(name.toLowerCase().replace(' ', '-'));
        organization.setTimeZone("Africa/Mogadishu");
        organization.setStatus(status);
        return organization;
    }

    private Role role() {
        Role role = new Role();
        role.setName(RoleName.ADMIN);
        role.setDataScope(DataScope.ALL);
        role.setPermissions(Set.of(
                permission(PermissionName.DASHBOARD_VIEW),
                permission(PermissionName.CUSTOMER_VIEW),
                permission(PermissionName.PERMISSION_MANAGE)
        ));
        return role;
    }

    private Permission permission(PermissionName name) {
        Permission permission = new Permission();
        permission.setName(name);
        return permission;
    }
}
