package com.crm.backend.publicapi;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PublicApiAuditService {

    private static final String ENTITY_TYPE = "PUBLIC_API_KEY";

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    public PublicApiAuditService(
            AuditLogService auditLogService,
            OrganizationRepository organizationRepository,
            ObjectMapper objectMapper
    ) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.objectMapper = objectMapper;
    }

    public void logForOrganization(
            Long organizationId,
            Long actorUserId,
            PublicApiAuditAction action,
            Long apiKeyId,
            Map<String, ?> details
    ) {
        Organization organization = organizationRepository
                .getReferenceById(organizationId);
        auditLogService.logForOrganization(
                organization,
                actorUserId,
                action.name(),
                ENTITY_TYPE,
                apiKeyId,
                writeDetails(details)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPlatformAuthenticationFailure() {
        auditLogService.log(
                null,
                PublicApiAuditAction.PUBLIC_API_AUTHENTICATION_FAILED.name(),
                ENTITY_TYPE,
                null,
                writeDetails(Map.of("reason", "invalid_credentials"))
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEventForOrganization(
            Long organizationId,
            PublicApiAuditAction action,
            Long apiKeyId,
            Map<String, ?> details
    ) {
        logForOrganization(
                organizationId,
                null,
                action,
                apiKeyId,
                details
        );
    }

    public Map<String, Object> details(Object... values) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();

        for (int index = 0; index < values.length; index += 2) {
            Object value = values[index + 1];
            if (value != null) {
                details.put(values[index].toString(), value);
            }
        }

        return details;
    }

    private String writeDetails(Map<String, ?> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException exception) {
            return "{}";
        }
    }
}
