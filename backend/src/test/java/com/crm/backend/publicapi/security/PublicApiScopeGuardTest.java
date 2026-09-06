package com.crm.backend.publicapi.security;

import com.crm.backend.publicapi.PublicApiAuditAction;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.PublicApiScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicApiScopeGuardTest {

    @AfterEach
    void tearDown() {
        PublicApiContextHolder.clear();
    }

    @Test
    void shouldAllowGrantedScopeAndAuditDeniedScope() {
        PublicApiAuditService auditService = mock(PublicApiAuditService.class);
        PublicApiScopeGuard guard = new PublicApiScopeGuard(auditService);
        PublicApiContextHolder.set(new PublicApiPrincipal(
                5L,
                42L,
                "Integration",
                Set.of(PublicApiScope.CUSTOMERS_READ),
                60
        ));

        assertDoesNotThrow(() ->
                guard.require(PublicApiScope.CUSTOMERS_READ)
        );
        assertThrows(
                AccessDeniedException.class,
                () -> guard.require(PublicApiScope.LEADS_READ)
        );
        verify(auditService).logSecurityEventForOrganization(
                eq(42L),
                eq(PublicApiAuditAction.PUBLIC_API_SCOPE_DENIED),
                eq(5L),
                anyMap()
        );
        verify(auditService).details("requiredScope", "leads:read");
    }
}
