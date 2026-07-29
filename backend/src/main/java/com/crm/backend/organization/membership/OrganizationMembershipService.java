package com.crm.backend.organization.membership;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.dto.CreateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.OrganizationMembershipResponse;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleRepository;
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

    public OrganizationMembershipService(
            OrganizationMembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganizationMembershipMapper membershipMapper,
            AuditLogService auditLogService
    ) {
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.membershipMapper = membershipMapper;
        this.auditLogService = auditLogService;
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