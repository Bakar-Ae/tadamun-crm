package com.crm.backend.organization.membership;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.dto.CreateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.DeactivateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.OrganizationMembershipResponse;
import com.crm.backend.organization.membership.dto.UpdateOrganizationMembershipRequest;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class OrganizationMembershipService {

    private static final Logger log =
            LoggerFactory.getLogger(OrganizationMembershipService.class);

    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationMembershipMapper membershipMapper;
    private final AuditLogService auditLogService;
    private final OrganizationRolePolicy organizationRolePolicy;

    public OrganizationMembershipService(
            OrganizationMembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganizationMembershipMapper membershipMapper,
            AuditLogService auditLogService,
            OrganizationRolePolicy organizationRolePolicy
    ) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.membershipMapper = membershipMapper;
        this.auditLogService = auditLogService;
        this.organizationRolePolicy = organizationRolePolicy;
    }

    @Transactional
    public OrganizationMembershipResponse createMembership(
            Long organizationId,
            CreateOrganizationMembershipRequest request,
            Long actorUserId
    ) {
        Organization organization =
                findActiveOrganizationOrThrow(organizationId);

        User user = findActiveUserOrThrow(
                request.userId(),
                "User"
        );

        findActiveUserOrThrow(actorUserId, "Actor");

        if (membershipRepository.existsByOrganizationIdAndUserId(
                organizationId,
                user.getId()
        )) {
            throw new IllegalArgumentException(
                    "User already belongs to this organization"
            );
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found"));

        OrganizationMembership membership =
                new OrganizationMembership();

        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setRole(role);
        membership.setStatus(OrganizationMembershipStatus.ACTIVE);

        OrganizationMembership savedMembership;

        try {
            savedMembership =
                    membershipRepository.saveAndFlush(membership);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "User already belongs to this organization"
            );
        }

        auditLogService.log(
                actorUserId,
                "ORGANIZATION_MEMBERSHIP_CREATED",
                "ORGANIZATION_MEMBERSHIP",
                savedMembership.getId(),
                "{\"organizationId\":" + organization.getId()
                        + ",\"userId\":" + user.getId()
                        + ",\"role\":\"" + role.getName() + "\"}"
        );

        log.info(
                "Organization membership created. membershipId={}, "
                        + "organizationId={}, userId={}, actorUserId={}, role={}",
                savedMembership.getId(),
                organization.getId(),
                user.getId(),
                actorUserId,
                role.getName()
        );

        return membershipMapper.toResponse(savedMembership);
    }

    @Transactional(readOnly = true)
    public OrganizationMembershipResponse getMembershipById(Long id) {
        return membershipMapper.toResponse(
                findMembershipOrThrow(id)
        );
    }

    @Transactional(readOnly = true)
    public Page<OrganizationMembershipResponse> getOrganizationMembers(
            Long organizationId,
            Pageable pageable
    ) {
        findOrganizationOrThrow(organizationId);

        return membershipRepository
                .findByOrganizationId(organizationId, pageable)
                .map(membershipMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrganizationMembershipResponse> getCurrentOrganizationMembers(
            Pageable pageable
    ) {
        Long organizationId = TenantContextHolder.getRequired()
                .organizationId();

        return membershipRepository
                .findByOrganizationId(organizationId, pageable)
                .map(membershipMapper::toResponse);
    }

    @Transactional
    public OrganizationMembershipResponse updateMembershipRole(
            Long membershipId,
            UpdateOrganizationMembershipRequest request
    ) {
        TenantContext context = TenantContextHolder.getRequired();
        OrganizationMembership membership = findCurrentMembershipOrThrow(
                membershipId,
                context.organizationId()
        );

        requireDifferentMember(context, membership);
        requireActiveMembership(membership);
        requireMatchingVersion(membership, request.version());
        organizationRolePolicy.requireCanManage(
                context.roleName(),
                membership.getRole().getName()
        );
        organizationRolePolicy.requireCanAssign(
                context.roleName(),
                request.role()
        );

        RoleName previousRole = membership.getRole().getName();

        if (previousRole == request.role()) {
            return membershipMapper.toResponse(membership);
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found"));

        membership.setRole(role);
        OrganizationMembership saved = membershipRepository
                .saveAndFlush(membership);

        auditLogService.log(
                context.userId(),
                "ORGANIZATION_MEMBERSHIP_ROLE_UPDATED",
                "ORGANIZATION_MEMBERSHIP",
                saved.getId(),
                "{\"userId\":" + saved.getUser().getId()
                        + ",\"previousRole\":\"" + previousRole
                        + "\",\"role\":\"" + request.role() + "\"}"
        );

        return membershipMapper.toResponse(saved);
    }

    @Transactional
    public OrganizationMembershipResponse deactivateMembership(
            Long membershipId,
            DeactivateOrganizationMembershipRequest request
    ) {
        TenantContext context = TenantContextHolder.getRequired();
        OrganizationMembership membership = findCurrentMembershipOrThrow(
                membershipId,
                context.organizationId()
        );

        requireDifferentMember(context, membership);
        requireActiveMembership(membership);
        requireMatchingVersion(membership, request.version());
        organizationRolePolicy.requireCanManage(
                context.roleName(),
                membership.getRole().getName()
        );

        membership.setStatus(OrganizationMembershipStatus.INACTIVE);
        OrganizationMembership saved = membershipRepository
                .saveAndFlush(membership);

        auditLogService.log(
                context.userId(),
                "ORGANIZATION_MEMBERSHIP_DEACTIVATED",
                "ORGANIZATION_MEMBERSHIP",
                saved.getId(),
                "{\"userId\":" + saved.getUser().getId()
                        + ",\"role\":\"" + saved.getRole().getName()
                        + "\"}"
        );

        return membershipMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembershipResponse>
    getActiveMembershipsForUser(Long userId) {
        findActiveUserOrThrow(userId, "User");

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
                .map(membershipMapper::toResponse)
                .toList();
    }

    private OrganizationMembership findMembershipOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Organization membership is required"
            );
        }

        return membershipRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Organization membership not found"
                        ));
    }

    private OrganizationMembership findCurrentMembershipOrThrow(
            Long membershipId,
            Long organizationId
    ) {
        if (membershipId == null) {
            throw new IllegalArgumentException(
                    "Organization membership is required"
            );
        }

        return membershipRepository
                .findByIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Organization membership not found"
                        ));
    }

    private void requireDifferentMember(
            TenantContext context,
            OrganizationMembership membership
    ) {
        if (Objects.equals(context.membershipId(), membership.getId())) {
            throw new IllegalArgumentException(
                    "You cannot change your own workspace access"
            );
        }
    }

    private void requireActiveMembership(
            OrganizationMembership membership
    ) {
        if (membership.getStatus() != OrganizationMembershipStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only active memberships can be updated"
            );
        }
    }

    private void requireMatchingVersion(
            OrganizationMembership membership,
            Long requestVersion
    ) {
        if (!Objects.equals(membership.getVersion(), requestVersion)) {
            throw new IllegalArgumentException(
                    "Membership was updated by someone else. Refresh and try again"
            );
        }
    }

    private Organization findOrganizationOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Organization is required"
            );
        }

        return organizationRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Organization not found"
                        ));
    }

    private Organization findActiveOrganizationOrThrow(Long id) {
        Organization organization = findOrganizationOrThrow(id);

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Organization must be active"
            );
        }

        return organization;
    }

    private User findActiveUserOrThrow(Long id, String label) {
        if (id == null) {
            throw new IllegalArgumentException(
                    label + " user is required"
            );
        }

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                label + " user not found"
                        ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    label + " user must be active"
            );
        }

        return user;
    }
}
