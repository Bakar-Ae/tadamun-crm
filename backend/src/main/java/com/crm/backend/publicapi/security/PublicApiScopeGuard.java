package com.crm.backend.publicapi.security;

import com.crm.backend.publicapi.PublicApiAuditAction;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.PublicApiScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PublicApiScopeGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            PublicApiScopeGuard.class
    );

    private final PublicApiAuditService auditService;

    public PublicApiScopeGuard(PublicApiAuditService auditService) {
        this.auditService = auditService;
    }

    public void require(PublicApiScope requiredScope) {
        PublicApiPrincipal principal = PublicApiContextHolder.getRequired();

        if (principal.scopes().contains(requiredScope)) {
            return;
        }

        try {
            auditService.logSecurityEventForOrganization(
                    principal.organizationId(),
                    PublicApiAuditAction.PUBLIC_API_SCOPE_DENIED,
                    principal.apiKeyId(),
                    auditService.details(
                            "requiredScope",
                            requiredScope.getValue()
                    )
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist public API scope audit");
        }

        throw new AccessDeniedException("Public API scope is required");
    }
}
