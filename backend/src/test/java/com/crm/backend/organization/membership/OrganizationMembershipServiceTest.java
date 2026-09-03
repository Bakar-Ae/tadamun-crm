package com.crm.backend.organization.membership;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.dto.CreateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.DeactivateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.OrganizationMembershipResponse;
import com.crm.backend.organization.membership.dto.UpdateOrganizationMembershipRequest;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationMembershipServiceTest {

    private OrganizationMembershipRepository membershipRepository;
    private OrganizationRepository organizationRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private AuditLogService auditLogService;
    private OrganizationMembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipRepository =
                mock(OrganizationMembershipRepository.class);
        organizationRepository =
                mock(OrganizationRepository.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        auditLogService = mock(AuditLogService.class);

        membershipService = new OrganizationMembershipService(
                membershipRepository,
                organizationRepository,
                userRepository,
                roleRepository,
                new OrganizationMembershipMapper(),
                auditLogService,
                new OrganizationRolePolicy()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createMembershipShouldCreateActiveMembership() {
        Organization organization =
                organization(10L, OrganizationStatus.ACTIVE);

        User user = activeUser(1L, "Sales User");
        User actor = activeUser(2L, "Administrator");
        Role role = role(3L, RoleName.SALES_REP);

        CreateOrganizationMembershipRequest request =
                new CreateOrganizationMembershipRequest(
                        1L,
                        RoleName.SALES_REP
                );

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(actor));
        when(membershipRepository
                .existsByOrganizationIdAndUserId(10L, 1L))
                .thenReturn(false);
        when(roleRepository.findByName(RoleName.SALES_REP))
                .thenReturn(Optional.of(role));

        when(membershipRepository.saveAndFlush(
                any(OrganizationMembership.class)
        )).thenAnswer(invocation -> {
            OrganizationMembership membership =
                    invocation.getArgument(0);
            membership.setId(100L);
            membership.setVersion(0L);
            return membership;
        });

        OrganizationMembershipResponse response =
                membershipService.createMembership(
                        10L,
                        request,
                        2L
                );

        assertEquals(100L, response.id());
        assertEquals(10L, response.organizationId());
        assertEquals(1L, response.userId());
        assertEquals(RoleName.SALES_REP, response.role());
        assertEquals(
                OrganizationMembershipStatus.ACTIVE,
                response.status()
        );

        verify(auditLogService).log(
                2L,
                "ORGANIZATION_MEMBERSHIP_CREATED",
                "ORGANIZATION_MEMBERSHIP",
                100L,
                "{\"organizationId\":10,"
                        + "\"userId\":1,"
                        + "\"role\":\"SALES_REP\"}"
        );
    }

    @Test
    void createMembershipShouldRejectDuplicateMembership() {
        prepareValidMembershipDependencies();

        when(membershipRepository
                .existsByOrganizationIdAndUserId(10L, 1L))
                .thenReturn(true);

        CreateOrganizationMembershipRequest request =
                new CreateOrganizationMembershipRequest(
                        1L,
                        RoleName.SALES_REP
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> membershipService.createMembership(
                        10L,
                        request,
                        2L
                )
        );

        assertEquals(
                "User already belongs to this organization",
                exception.getMessage()
        );

        verify(
                membershipRepository,
                never()
        ).saveAndFlush(any(OrganizationMembership.class));
    }

    @Test
    void createMembershipShouldRejectInactiveOrganization() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(
                        organization(
                                10L,
                                OrganizationStatus.SUSPENDED
                        )
                ));

        CreateOrganizationMembershipRequest request =
                new CreateOrganizationMembershipRequest(
                        1L,
                        RoleName.SALES_REP
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> membershipService.createMembership(
                        10L,
                        request,
                        2L
                )
        );

        assertEquals(
                "Organization must be active",
                exception.getMessage()
        );
    }

    @Test
    void createMembershipShouldRejectInactiveUser() {
        Organization organization =
                organization(10L, OrganizationStatus.ACTIVE);

        User user = activeUser(1L, "Sales User");
        user.setStatus(UserStatus.INACTIVE);

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        CreateOrganizationMembershipRequest request =
                new CreateOrganizationMembershipRequest(
                        1L,
                        RoleName.SALES_REP
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> membershipService.createMembership(
                        10L,
                        request,
                        2L
                )
        );

        assertEquals(
                "User user must be active",
                exception.getMessage()
        );
    }

    @Test
    void createMembershipShouldRejectMissingRole() {
        prepareValidMembershipDependencies();

        when(roleRepository.findByName(RoleName.SALES_REP))
                .thenReturn(Optional.empty());

        CreateOrganizationMembershipRequest request =
                new CreateOrganizationMembershipRequest(
                        1L,
                        RoleName.SALES_REP
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> membershipService.createMembership(
                        10L,
                        request,
                        2L
                )
        );

        assertEquals("Role not found", exception.getMessage());
    }

    @Test
    void activeMembershipListShouldExcludeSuspendedOrganization() {
        User user = activeUser(1L, "Sales User");
        Role role = role(3L, RoleName.SALES_REP);

        OrganizationMembership activeMembership = membership(
                100L,
                organization(10L, OrganizationStatus.ACTIVE),
                user,
                role
        );

        OrganizationMembership suspendedOrganizationMembership =
                membership(
                        101L,
                        organization(
                                11L,
                                OrganizationStatus.SUSPENDED
                        ),
                        user,
                        role
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(membershipRepository
                .findByUserIdAndStatusOrderByOrganizationNameAsc(
                        1L,
                        OrganizationMembershipStatus.ACTIVE
                ))
                .thenReturn(List.of(
                        activeMembership,
                        suspendedOrganizationMembership
                ));

        List<OrganizationMembershipResponse> responses =
                membershipService.getActiveMembershipsForUser(1L);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.getFirst().organizationId());
    }

    @Test
    void ownerShouldUpdateAnotherMembersRole() {
        User targetUser = activeUser(1L, "Sales User");
        OrganizationMembership target = membership(
                100L,
                organization(10L, OrganizationStatus.ACTIVE),
                targetUser,
                role(3L, RoleName.SALES_REP)
        );
        Role managerRole = role(4L, RoleName.MANAGER);
        setTenantContext(999L, 2L, RoleName.OWNER);

        when(membershipRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.of(target));
        when(roleRepository.findByName(RoleName.MANAGER))
                .thenReturn(Optional.of(managerRole));
        when(membershipRepository.saveAndFlush(target))
                .thenReturn(target);

        OrganizationMembershipResponse response = membershipService
                .updateMembershipRole(
                        100L,
                        new UpdateOrganizationMembershipRequest(
                                RoleName.MANAGER,
                                0L
                        )
                );

        assertEquals(RoleName.MANAGER, response.role());
        verify(auditLogService).log(
                eq(2L),
                eq("ORGANIZATION_MEMBERSHIP_ROLE_UPDATED"),
                eq("ORGANIZATION_MEMBERSHIP"),
                eq(100L),
                anyString()
        );
    }

    @Test
    void memberShouldNotDeactivateOwnMembership() {
        OrganizationMembership membership = membership(
                100L,
                organization(10L, OrganizationStatus.ACTIVE),
                activeUser(1L, "Owner"),
                role(1L, RoleName.OWNER)
        );
        setTenantContext(100L, 1L, RoleName.OWNER);

        when(membershipRepository.findByIdAndOrganizationId(100L, 10L))
                .thenReturn(Optional.of(membership));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> membershipService.deactivateMembership(
                        100L,
                        new DeactivateOrganizationMembershipRequest(0L)
                )
        );

        assertEquals(
                "You cannot change your own workspace access",
                exception.getMessage()
        );
        verify(membershipRepository, never())
                .saveAndFlush(any(OrganizationMembership.class));
    }

    private void prepareValidMembershipDependencies() {
        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(
                        organization(10L, OrganizationStatus.ACTIVE)
                ));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(
                        activeUser(1L, "Sales User")
                ));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(
                        activeUser(2L, "Administrator")
                ));
    }

    private Organization organization(
            Long id,
            OrganizationStatus status
    ) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Organization " + id);
        organization.setSlug("organization-" + id);
        organization.setStatus(status);
        organization.setTimeZone("Africa/Mogadishu");
        return organization;
    }

    private User activeUser(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail("user" + id + "@crm.com");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Role role(Long id, RoleName roleName) {
        Role role = new Role();
        role.setId(id);
        role.setName(roleName);
        return role;
    }

    private OrganizationMembership membership(
            Long id,
            Organization organization,
            User user,
            Role role
    ) {
        OrganizationMembership membership =
                new OrganizationMembership();

        membership.setId(id);
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setRole(role);
        membership.setStatus(
                OrganizationMembershipStatus.ACTIVE
        );
        membership.setVersion(0L);

        return membership;
    }

    private void setTenantContext(
            Long membershipId,
            Long userId,
            RoleName roleName
    ) {
        TenantContextHolder.set(new TenantContext(
                10L,
                membershipId,
                userId,
                roleName,
                DataScope.ALL,
                null,
                Set.of()
        ));
    }
}
