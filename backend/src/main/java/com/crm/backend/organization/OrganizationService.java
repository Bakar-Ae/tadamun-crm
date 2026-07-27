package com.crm.backend.organization;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.dto.CreateOrganizationRequest;
import com.crm.backend.organization.dto.OrganizationResponse;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class OrganizationService {

    private static final Logger log =
            LoggerFactory.getLogger(OrganizationService.class);

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationMapper organizationMapper,
            AuditLogService auditLogService
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.organizationMapper = organizationMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request,
            Long creatorUserId
    ) {
        String name = requireText(
                request.name(),
                "Organization name is required"
        );

        String slug = normalizeSlug(request.slug());
        String timeZone = normalizeTimeZone(request.timeZone());

        if (organizationRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException(
                    "Organization slug already exists"
            );
        }

        User creator = findActiveCreatorOrThrow(creatorUserId);

        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(slug);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone(timeZone);
        organization.setCreatedByUser(creator);

        Organization savedOrganization;

        try {
            savedOrganization =
                    organizationRepository.saveAndFlush(organization);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "Organization slug already exists"
            );
        }

        auditLogService.log(
                creatorUserId,
                "ORGANIZATION_CREATED",
                "ORGANIZATION",
                savedOrganization.getId(),
                "{\"name\":\"" + savedOrganization.getName()
                        + "\",\"slug\":\"" + savedOrganization.getSlug() + "\"}"
        );

        log.info(
                "Organization created. organizationId={}, creatorUserId={}, slug={}",
                savedOrganization.getId(),
                creatorUserId,
                savedOrganization.getSlug()
        );

        return organizationMapper.toResponse(savedOrganization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(Long id) {
        return organizationMapper.toResponse(findOrganizationOrThrow(id));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);

        Organization organization = organizationRepository
                .findBySlug(normalizedSlug)
                .orElseThrow(() ->
                        new IllegalArgumentException("Organization not found"));

        return organizationMapper.toResponse(organization);
    }

    private Organization findOrganizationOrThrow(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Organization not found"));
    }

    private User findActiveCreatorOrThrow(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "Organization creator is required"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Creator user not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Creator user must be active"
            );
        }

        return user;
    }

    private String normalizeSlug(String value) {
        String slug = requireText(
                value,
                "Organization slug is required"
        ).toLowerCase(Locale.ROOT);

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                    "Organization slug is invalid"
            );
        }

        return slug;
    }

    private String normalizeTimeZone(String value) {
        String timeZone = requireText(
                value,
                "Organization time zone is required"
        );

        try {
            return ZoneId.of(timeZone).getId();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "Organization time zone is invalid"
            );
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}