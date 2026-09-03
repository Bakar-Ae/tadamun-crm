package com.crm.backend.security.tenant;

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
import com.crm.backend.user.User;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantResolutionServiceTest {

    private OrganizationMembershipRepository membershipRepository;
    private TenantResolutionService resolutionService;

    @BeforeEach
    void setUp() {
        membershipRepository =
                mock(OrganizationMembershipRepository.class);
        resolutionService =
                new TenantResolutionService(
                        membershipRepository,
                        new TenantPermissionPolicy()
                );
    }

    @Test
    void requestedActiveMembershipShouldResolveContext() {
        OrganizationMembership membership = membership(
                50L,
                10L,
                20L,
                OrganizationStatus.ACTIVE
        );

        when(membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        10L,
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));

        TenantContext context =
                resolutionService.resolve(20L, 10L);

        assertEquals(10L, context.organizationId());
        assertEquals(50L, context.membershipId());
        assertEquals(20L, context.userId());
        assertEquals(RoleName.MANAGER, context.roleName());
        assertEquals(DataScope.TEAM, context.dataScope());
        assertEquals(
                Set.of(PermissionName.CUSTOMER_VIEW),
                context.permissions()
        );
    }

    @Test
    void singleActiveMembershipShouldBeSelectedForCompatibility() {
        OrganizationMembership membership = membership(
                50L,
                10L,
                20L,
                OrganizationStatus.ACTIVE
        );

        when(membershipRepository
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(List.of(membership));

        TenantContext context =
                resolutionService.resolve(20L, null);

        assertEquals(10L, context.organizationId());
    }

    @Test
    void multipleActiveMembershipsShouldRequireExplicitContext() {
        when(membershipRepository
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(List.of(
                        membership(
                                50L,
                                10L,
                                20L,
                                OrganizationStatus.ACTIVE
                        ),
                        membership(
                                51L,
                                11L,
                                20L,
                                OrganizationStatus.ACTIVE
                        )
                ));

        TenantAccessException exception = assertThrows(
                TenantAccessException.class,
                () -> resolutionService.resolve(20L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "TENANT_CONTEXT_REQUIRED",
                exception.getErrorCode()
        );
    }

    @Test
    void suspendedOrganizationShouldBeRejected() {
        OrganizationMembership membership = membership(
                50L,
                10L,
                20L,
                OrganizationStatus.SUSPENDED
        );

        when(membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        10L,
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));

        TenantAccessException exception = assertThrows(
                TenantAccessException.class,
                () -> resolutionService.resolve(20L, 10L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals(
                "ORGANIZATION_UNAVAILABLE",
                exception.getErrorCode()
        );
    }

    @Test
    void requestedOrganizationOutsideMembershipShouldBeRejectedWithoutFallback() {
        when(membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        99L,
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.empty());

        TenantAccessException exception = assertThrows(
                TenantAccessException.class,
                () -> resolutionService.resolve(20L, 99L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals(
                "ORGANIZATION_ACCESS_DENIED",
                exception.getErrorCode()
        );
        verify(membershipRepository, never())
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                );
    }

    @Test
    void inactiveUserShouldBeRejected() {
        OrganizationMembership membership = membership(
                50L,
                10L,
                20L,
                OrganizationStatus.ACTIVE
        );
        membership.getUser().setStatus(UserStatus.INACTIVE);

        when(membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        10L,
                        20L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(Optional.of(membership));

        TenantAccessException exception = assertThrows(
                TenantAccessException.class,
                () -> resolutionService.resolve(20L, 10L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals(
                "ORGANIZATION_ACCESS_DENIED",
                exception.getErrorCode()
        );
    }

    private OrganizationMembership membership(
            Long membershipId,
            Long organizationId,
            Long userId,
            OrganizationStatus organizationStatus
    ) {
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setName("Organization " + organizationId);
        organization.setStatus(organizationStatus);

        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);

        Permission permission = new Permission();
        permission.setName(PermissionName.CUSTOMER_VIEW);

        Permission platformPermission = new Permission();
        platformPermission.setName(PermissionName.PERMISSION_MANAGE);

        Role role = new Role();
        role.setName(RoleName.MANAGER);
        role.setDataScope(DataScope.TEAM);
        role.setPermissions(Set.of(permission, platformPermission));

        Role globalRole = new Role();
        globalRole.setName(RoleName.ADMIN);
        globalRole.setDataScope(DataScope.ALL);
        user.setRole(globalRole);

        OrganizationMembership membership =
                new OrganizationMembership();
        membership.setId(membershipId);
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setRole(role);
        membership.setStatus(OrganizationMembershipStatus.ACTIVE);

        return membership;
    }
}
