package com.crm.backend.audit;

import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        currentOrganizationProvider =
                mock(CurrentOrganizationProvider.class);

        auditLogService = new AuditLogService(
                auditLogRepository,
                mock(UserRepository.class),
                new AuditLogMapper(),
                currentOrganizationProvider
        );
    }

    @Test
    void logShouldCreateOrganizationEventInsideTenantContext() {
        Organization organization = new Organization();
        organization.setId(12L);
        when(currentOrganizationProvider
                .getOptionalOrganizationReference())
                .thenReturn(Optional.of(organization));

        auditLogService.log(
                null,
                "CUSTOMER_CREATED",
                "CUSTOMER",
                42L,
                "{}"
        );

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertEquals(
                organization,
                captor.getValue().getOrganization()
        );
        assertEquals(
                AuditLogScope.ORGANIZATION,
                captor.getValue().getScope()
        );
    }

    @Test
    void logShouldCreatePlatformEventWithoutTenantContext() {
        when(currentOrganizationProvider
                .getOptionalOrganizationReference())
                .thenReturn(Optional.empty());

        auditLogService.log(
                null,
                "LOGIN_FAILED",
                "AUTH",
                null,
                "{}"
        );

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertNull(captor.getValue().getOrganization());
        assertEquals(
                AuditLogScope.PLATFORM,
                captor.getValue().getScope()
        );
    }

    @Test
    void getAuditLogsShouldQueryOnlyCurrentOrganization() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(currentOrganizationProvider.getOrganizationId())
                .thenReturn(12L);
        when(auditLogRepository.searchAuditLogsInOrganization(
                12L,
                null,
                null,
                null,
                null,
                pageable
        )).thenReturn(Page.empty(pageable));

        auditLogService.getAuditLogs(pageable);

        verify(auditLogRepository)
                .searchAuditLogsInOrganization(
                        12L,
                        null,
                        null,
                        null,
                        null,
                        pageable
                );
    }
}
