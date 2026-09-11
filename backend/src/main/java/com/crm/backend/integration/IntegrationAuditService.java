package com.crm.backend.integration;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.OrganizationRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class IntegrationAuditService {

    private static final String ENTITY_TYPE = "INTEGRATION_CONNECTION";

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    public IntegrationAuditService(
            AuditLogService auditLogService,
            OrganizationRepository organizationRepository,
            ObjectMapper objectMapper
    ) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.objectMapper = objectMapper;
    }

    public void log(
            Long organizationId,
            Long actorUserId,
            IntegrationAuditAction action,
            Long connectionId,
            Map<String, ?> details
    ) {
        auditLogService.logForOrganization(
                organizationRepository.getReferenceById(organizationId),
                actorUserId,
                action.name(),
                ENTITY_TYPE,
                connectionId,
                writeDetails(details)
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
