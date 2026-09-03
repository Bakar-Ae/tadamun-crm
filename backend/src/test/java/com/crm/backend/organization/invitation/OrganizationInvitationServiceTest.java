package com.crm.backend.organization.invitation;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.email.EmailService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.invitation.dto.AcceptOrganizationInvitationRequest;
import com.crm.backend.organization.invitation.dto.CreateOrganizationInvitationRequest;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrganizationInvitationServiceTest {

    private OrganizationInvitationRepository invitationRepository;
    private OrganizationMembershipRepository membershipRepository;
    private OrganizationRepository organizationRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private OrganizationInvitationTokenService tokenService;
    private AuditLogService auditLogService;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private OrganizationInvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(OrganizationInvitationRepository.class);
        membershipRepository = mock(OrganizationMembershipRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        tokenService = mock(OrganizationInvitationTokenService.class);
        auditLogService = mock(AuditLogService.class);
        emailService = mock(EmailService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        invitationService = new OrganizationInvitationService(
                invitationRepository,
                membershipRepository,
                organizationRepository,
                userRepository,
                roleRepository,
                tokenService,
                new OrganizationInvitationMapper(),
                auditLogService,
                emailService,
                passwordEncoder,
                "http://localhost:5173/"
        );

        TenantContextHolder.set(new TenantContext(
                10L,
                20L,
                1L,
                RoleName.ADMIN,
                DataScope.ALL,
                null,
                Set.of(PermissionName.USER_CREATE)
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createInvitationShouldNormalizeEmailAndUseCurrentOrganization() {
        Organization organization = organization(10L);
        Role role = role(RoleName.SALES_REP);
        User actor = user(1L, "System Administrator");

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(userRepository.findByEmail("sales@example.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository
                .findByOrganizationIdAndEmailIgnoreCaseAndStatus(
                        10L,
                        "sales@example.com",
                        OrganizationInvitationStatus.PENDING
                )).thenReturn(List.of());
        when(roleRepository.findByName(RoleName.SALES_REP))
                .thenReturn(Optional.of(role));
        when(userRepository.getReferenceById(1L)).thenReturn(actor);
        when(tokenService.generate()).thenReturn(
                new GeneratedInvitationToken("raw-token", "stored-hash")
        );
        when(invitationRepository.saveAndFlush(any(OrganizationInvitation.class)))
                .thenAnswer(invocation -> {
                    OrganizationInvitation invitation = invocation.getArgument(0);
                    invitation.setId(100L);
                    return invitation;
                });

        var response = invitationService.createInvitation(
                new CreateOrganizationInvitationRequest(
                        "  Sales@Example.com ",
                        RoleName.SALES_REP
                )
        );

        assertEquals(10L, response.organizationId());
        assertEquals("sales@example.com", response.email());
        assertEquals(RoleName.SALES_REP, response.role());

        ArgumentCaptor<OrganizationInvitation> invitationCaptor =
                ArgumentCaptor.forClass(OrganizationInvitation.class);
        verify(invitationRepository).saveAndFlush(invitationCaptor.capture());

        assertEquals(
                "stored-hash",
                invitationCaptor.getValue().getTokenHash()
        );
        verify(emailService).sendOrganizationInvitationEmail(
                eq("sales@example.com"),
                eq("Tadamun"),
                eq("System Administrator"),
                eq("Sales Rep"),
                eq("http://localhost:5173/accept-invitation?token=raw-token"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void createInvitationShouldRejectExistingPendingInvitation() {
        Organization organization = organization(10L);
        OrganizationInvitation pending = new OrganizationInvitation();
        pending.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(userRepository.findByEmail("sales@example.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository
                .findByOrganizationIdAndEmailIgnoreCaseAndStatus(
                        10L,
                        "sales@example.com",
                        OrganizationInvitationStatus.PENDING
                )).thenReturn(List.of(pending));

        assertThrows(
                IllegalArgumentException.class,
                () -> invitationService.createInvitation(
                        new CreateOrganizationInvitationRequest(
                                "sales@example.com",
                                RoleName.SALES_REP
                        )
                )
        );

        verify(invitationRepository, never())
                .saveAndFlush(any(OrganizationInvitation.class));
    }

    @Test
    void revokeInvitationShouldNotFindAnotherOrganizationsInvitation() {
        when(invitationRepository.findByIdAndOrganizationId(99L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> invitationService.revokeInvitation(99L)
        );
    }
    @Test
    void acceptInvitationShouldAddExistingActiveUser() {
        Organization organization = organization(10L);
        Role role = role(RoleName.SALES_REP);
        OrganizationInvitation invitation =
                pendingInvitation(organization, role);

        User existingUser = user(2L, "Sales User");
        existingUser.setEmail("sales@example.com");
        existingUser.setStatus(UserStatus.ACTIVE);

        when(tokenService.hash("raw-token")).thenReturn("stored-hash");
        when(invitationRepository.findByTokenHash("stored-hash"))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("sales@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(membershipRepository.existsByOrganizationIdAndUserId(10L, 2L))
                .thenReturn(false);

        var response = invitationService.acceptInvitation(
                new AcceptOrganizationInvitationRequest(
                        "raw-token",
                        null,
                        null,
                        null
                )
        );

        assertEquals(10L, response.organizationId());
        assertEquals(2L, response.userId());
        assertEquals(
                OrganizationInvitationStatus.ACCEPTED,
                invitation.getStatus()
        );
        assertEquals(existingUser, invitation.getAcceptedByUser());

        verify(membershipRepository)
                .saveAndFlush(any(OrganizationMembership.class));
        verify(passwordEncoder, never()).encode(any());
        verify(auditLogService).logForOrganization(
                eq(organization),
                eq(2L),
                eq("ORGANIZATION_INVITATION_ACCEPTED"),
                eq("ORGANIZATION_INVITATION"),
                eq(100L),
                any(String.class)
        );
    }
    @Test
    void acceptInvitationShouldRejectExpiredInvitation() {
        Organization organization = organization(10L);
        Role role = role(RoleName.SALES_REP);
        OrganizationInvitation invitation =
                pendingInvitation(organization, role);

        invitation.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(tokenService.hash("expired-token")).thenReturn("expired-hash");
        when(invitationRepository.findByTokenHash("expired-hash"))
                .thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invitationService.acceptInvitation(
                        new AcceptOrganizationInvitationRequest(
                                "expired-token",
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "Invitation is invalid or no longer available",
                exception.getMessage()
        );

        verify(membershipRepository, never())
                .saveAndFlush(any(OrganizationMembership.class));
    }
    @Test
    void acceptInvitationShouldCreateNewUserAndMembership() {
        Organization organization = organization(10L);
        Role role = role(RoleName.SALES_REP);
        OrganizationInvitation invitation =
                pendingInvitation(organization, role);

        when(tokenService.hash("new-user-token"))
                .thenReturn("new-user-hash");
        when(invitationRepository.findByTokenHash("new-user-hash"))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("sales@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPassword@2026"))
                .thenReturn("encoded-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(3L);
                    return savedUser;
                });

        when(membershipRepository
                .existsByOrganizationIdAndUserId(10L, 3L))
                .thenReturn(false);

        var response = invitationService.acceptInvitation(
                new AcceptOrganizationInvitationRequest(
                        "new-user-token",
                        "  New Sales User  ",
                        "StrongPassword@2026",
                        "StrongPassword@2026"
                )
        );

        assertEquals(3L, response.userId());
        assertEquals("sales@example.com", response.email());
        assertEquals(
                OrganizationInvitationStatus.ACCEPTED,
                invitation.getStatus()
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(userCaptor.capture());

        User createdUser = userCaptor.getValue();

        assertEquals("New Sales User", createdUser.getFullName());
        assertEquals("sales@example.com", createdUser.getEmail());
        assertEquals("encoded-password", createdUser.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, createdUser.getStatus());
        assertEquals(role, createdUser.getRole());

        verify(membershipRepository)
                .saveAndFlush(any(OrganizationMembership.class));
    }

    private Organization organization(Long id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Tadamun");
        organization.setStatus(OrganizationStatus.ACTIVE);
        return organization;
    }

    private Role role(RoleName name) {
        Role role = new Role();
        role.setId(2L);
        role.setName(name);
        return role;
    }

    private User user(Long id, String fullName) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        return user;
    }
    private OrganizationInvitation pendingInvitation(
            Organization organization,
            Role role
    ) {
        OrganizationInvitation invitation = new OrganizationInvitation();
        invitation.setId(100L);
        invitation.setOrganization(organization);
        invitation.setEmail("sales@example.com");
        invitation.setRole(role);
        invitation.setStatus(OrganizationInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(2));
        return invitation;
    }

}
