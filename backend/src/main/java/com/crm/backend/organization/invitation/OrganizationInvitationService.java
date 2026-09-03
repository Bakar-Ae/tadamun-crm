package com.crm.backend.organization.invitation;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.email.EmailService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.invitation.dto.*;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OrganizationInvitationService {

    private static final long INVITATION_VALIDITY_HOURS = 72;

    private final OrganizationInvitationRepository invitationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationInvitationTokenService tokenService;
    private final OrganizationInvitationMapper invitationMapper;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final PasswordEncoder passwordEncoder;

    public OrganizationInvitationService(
            OrganizationInvitationRepository invitationRepository,
            OrganizationMembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganizationInvitationTokenService tokenService,
            OrganizationInvitationMapper invitationMapper,
            AuditLogService auditLogService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.invitationMapper = invitationMapper;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OrganizationInvitationResponse createInvitation(
            CreateOrganizationInvitationRequest request
    ) {
        TenantContext context = TenantContextHolder.getRequired();
        Organization organization = findActiveOrganization(
                context.organizationId()
        );
        String email = normalizeEmail(request.email());

        ensureUserIsNotAlreadyMember(organization.getId(), email);
        expireOrRejectExistingInvitations(organization.getId(), email);

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found"));
        User actor = userRepository.getReferenceById(context.userId());
        GeneratedInvitationToken generatedToken = tokenService.generate();

        OrganizationInvitation invitation = new OrganizationInvitation();
        invitation.setOrganization(organization);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setTokenHash(generatedToken.tokenHash());
        invitation.setStatus(OrganizationInvitationStatus.PENDING);
        invitation.setInvitedByUser(actor);
        invitation.setExpiresAt(
                LocalDateTime.now().plusHours(INVITATION_VALIDITY_HOURS)
        );

        OrganizationInvitation saved;

        try {
            saved = invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "A pending invitation already exists for this email"
            );
        }

        auditLogService.log(
                context.userId(),
                "ORGANIZATION_INVITATION_CREATED",
                "ORGANIZATION_INVITATION",
                saved.getId(),
                "{\"email\":\"" + email + "\",\"role\":\""
                        + role.getName() + "\"}"
        );

        emailService.sendOrganizationInvitationEmail(
                email,
                organization.getName(),
                actor.getFullName(),
                displayRoleName(role),
                buildInvitationLink(generatedToken.rawToken()),
                saved.getExpiresAt()
        );

        return invitationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationInvitationPreviewResponse previewInvitation(
            String rawToken
    ) {
        String tokenHash = tokenService.hash(rawToken);

        OrganizationInvitation invitation = invitationRepository
                .findFirstByTokenHash(tokenHash)
                .orElseThrow(this::invalidInvitation);

        validateUsableInvitation(invitation);

        return new OrganizationInvitationPreviewResponse(
                invitation.getOrganization().getName(),
                invitation.getEmail(),
                invitation.getRole().getName(),
                invitation.getExpiresAt(),
                !userRepository.existsByEmail(invitation.getEmail())
        );
    }

    @Transactional
    public OrganizationInvitationAcceptanceResponse acceptInvitation(
            AcceptOrganizationInvitationRequest request
    ) {
        String tokenHash = tokenService.hash(request.token());

        OrganizationInvitation invitation = invitationRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::invalidInvitation);

        validateUsableInvitation(invitation);

        User user = userRepository.findByEmail(invitation.getEmail())
                .map(this::requireActiveUser)
                .orElseGet(() -> createInvitedUser(invitation, request));

        if (membershipRepository.existsByOrganizationIdAndUserId(
                invitation.getOrganization().getId(),
                user.getId()
        )) {
            throw new IllegalArgumentException(
                    "User already belongs to this organization"
            );
        }

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(invitation.getOrganization());
        membership.setUser(user);
        membership.setRole(invitation.getRole());
        membership.setStatus(OrganizationMembershipStatus.ACTIVE);

        try {
            membershipRepository.saveAndFlush(membership);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "User already belongs to this organization"
            );
        }

        invitation.setStatus(OrganizationInvitationStatus.ACCEPTED);
        invitation.setAcceptedByUser(user);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.saveAndFlush(invitation);
        auditLogService.logForOrganization(
                invitation.getOrganization(),
                user.getId(),
                "ORGANIZATION_INVITATION_ACCEPTED",
                "ORGANIZATION_INVITATION",
                invitation.getId(),
                "{\"email\":\"" + invitation.getEmail() + "\"}"
        );

        return new OrganizationInvitationAcceptanceResponse(
                invitation.getOrganization().getId(),
                invitation.getOrganization().getName(),
                user.getId(),
                user.getEmail(),
                invitation.getRole().getName()
        );
    }



    @Transactional(readOnly = true)
    public Page<OrganizationInvitationResponse> getInvitations(
            Pageable pageable
    ) {
        Long organizationId = TenantContextHolder.getRequired()
                .organizationId();

        return invitationRepository
                .findByOrganizationId(organizationId, pageable)
                .map(invitationMapper::toResponse);
    }

    @Transactional
    public OrganizationInvitationResponse revokeInvitation(Long id) {
        TenantContext context = TenantContextHolder.getRequired();
        OrganizationInvitation invitation = invitationRepository
                .findByIdAndOrganizationId(id, context.organizationId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Organization invitation not found"
                        ));

        if (invitation.getStatus() != OrganizationInvitationStatus.PENDING
                || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Only pending invitations can be revoked"
            );
        }

        invitation.setStatus(OrganizationInvitationStatus.REVOKED);
        invitation.setRevokedAt(LocalDateTime.now());
        invitation.setRevokedByUser(
                userRepository.getReferenceById(context.userId())
        );

        auditLogService.log(
                context.userId(),
                "ORGANIZATION_INVITATION_REVOKED",
                "ORGANIZATION_INVITATION",
                invitation.getId(),
                "{\"email\":\"" + invitation.getEmail() + "\"}"
        );

        return invitationMapper.toResponse(invitation);
    }

    private Organization findActiveOrganization(Long organizationId) {
        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Organization not found"
                        ));

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Organization must be active"
            );
        }

        return organization;
    }

    private void ensureUserIsNotAlreadyMember(
            Long organizationId,
            String email
    ) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (membershipRepository.existsByOrganizationIdAndUserId(
                    organizationId,
                    user.getId()
            )) {
                throw new IllegalArgumentException(
                        "User already belongs to this organization"
                );
            }
        });
    }

    private void expireOrRejectExistingInvitations(
            Long organizationId,
            String email
    ) {
        List<OrganizationInvitation> existing = invitationRepository
                .findByOrganizationIdAndEmailIgnoreCaseAndStatus(
                        organizationId,
                        email,
                        OrganizationInvitationStatus.PENDING
                );

        LocalDateTime now = LocalDateTime.now();

        for (OrganizationInvitation invitation : existing) {
            if (invitation.getExpiresAt().isAfter(now)) {
                throw new IllegalArgumentException(
                        "A pending invitation already exists for this email"
                );
            }

            invitation.setStatus(OrganizationInvitationStatus.EXPIRED);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildInvitationLink(String rawToken) {
        String normalizedBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;

        return UriComponentsBuilder.fromUriString(normalizedBaseUrl)
                .path("/accept-invitation")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }

    private String displayRoleName(Role role) {
        String[] words = role.getName().name()
                .toLowerCase(Locale.ROOT)
                .split("_");
        StringBuilder displayName = new StringBuilder();

        for (String word : words) {
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }

            displayName.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return displayName.toString();
    }
    private void validateUsableInvitation(
            OrganizationInvitation invitation
    ) {
        boolean expired = !invitation.getExpiresAt()
                .isAfter(LocalDateTime.now());

        boolean organizationInactive =
                invitation.getOrganization().getStatus()
                        != OrganizationStatus.ACTIVE;

        if (invitation.getStatus() != OrganizationInvitationStatus.PENDING
                || expired
                || organizationInactive) {
            throw invalidInvitation();
        }
    }

    private IllegalArgumentException invalidInvitation() {
        return new IllegalArgumentException(
                "Invitation is invalid or no longer available"
        );
    }
    private User requireActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "The invited account is inactive"
            );
        }

        return user;
    }

    private User createInvitedUser(
            OrganizationInvitation invitation,
            AcceptOrganizationInvitationRequest request
    ) {
        String fullName = request.fullName() == null
                ? ""
                : request.fullName().trim();

        if (fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required"
            );
        }

        String password = request.password();

        if (password == null
                || password.length() < 8
                || password.length() > 100) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 100 characters"
            );
        }

        if (!password.equals(request.confirmPassword())) {
            throw new IllegalArgumentException(
                    "Passwords do not match"
            );
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(invitation.getEmail());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordChangeRequired(false);
        user.setRole(invitation.getRole());
        user.setStatus(UserStatus.ACTIVE);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "An account with this email already exists; retry the invitation"
            );
        }
    }
}
