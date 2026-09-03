package com.crm.backend.organization;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.dto.CreateOrganizationRequest;
import com.crm.backend.organization.dto.OrganizationResponse;
import com.crm.backend.organization.dto.UpdateOrganizationRequest;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationServiceTest {

    private OrganizationRepository organizationRepository;
    private UserRepository userRepository;
    private AuditLogService auditLogService;
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationRepository = mock(OrganizationRepository.class);
        userRepository = mock(UserRepository.class);
        auditLogService = mock(AuditLogService.class);

        organizationService = new OrganizationService(
                organizationRepository,
                userRepository,
                new OrganizationMapper(),
                auditLogService,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createOrganizationShouldCreateActiveOrganization() {
        CreateOrganizationRequest request =
                new CreateOrganizationRequest(
                        "Tadamun Business",
                        "tadamun-business",
                        "Africa/Mogadishu"
                );

        User creator = activeUser(1L);

        when(organizationRepository.existsBySlug("tadamun-business"))
                .thenReturn(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenAnswer(invocation -> {
                    Organization organization = invocation.getArgument(0);
                    organization.setId(10L);
                    organization.setVersion(0L);
                    return organization;
                });

        OrganizationResponse response =
                organizationService.createOrganization(request, 1L);

        assertEquals(10L, response.id());
        assertEquals("Tadamun Business", response.name());
        assertEquals("tadamun-business", response.slug());
        assertEquals(
                OrganizationStatus.ACTIVE,
                response.status()
        );
        assertEquals("Africa/Mogadishu", response.timeZone());
        assertEquals(1L, response.createdByUserId());

        verify(auditLogService).log(
                1L,
                "ORGANIZATION_CREATED",
                "ORGANIZATION",
                10L,
                "{\"name\":\"Tadamun Business\","
                        + "\"slug\":\"tadamun-business\"}"
        );
    }

    @Test
    void createOrganizationShouldRejectDuplicateSlug() {
        CreateOrganizationRequest request =
                new CreateOrganizationRequest(
                        "Tadamun Business",
                        "tadamun-business",
                        "Africa/Mogadishu"
                );

        when(organizationRepository.existsBySlug("tadamun-business"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.createOrganization(request, 1L)
        );

        assertEquals(
                "Organization slug already exists",
                exception.getMessage()
        );

        verify(
                organizationRepository,
                never()
        ).saveAndFlush(any(Organization.class));
    }

    @Test
    void createOrganizationShouldRejectInvalidTimeZone() {
        CreateOrganizationRequest request =
                new CreateOrganizationRequest(
                        "Tadamun Business",
                        "tadamun-business",
                        "Mars/Olympus"
                );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.createOrganization(request, 1L)
        );

        assertEquals(
                "Organization time zone is invalid",
                exception.getMessage()
        );
    }

    @Test
    void createOrganizationShouldRejectInactiveCreator() {
        CreateOrganizationRequest request =
                new CreateOrganizationRequest(
                        "Tadamun Business",
                        "tadamun-business",
                        "Africa/Mogadishu"
                );

        User creator = activeUser(1L);
        creator.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(creator));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.createOrganization(request, 1L)
        );

        assertEquals(
                "Creator user must be active",
                exception.getMessage()
        );
    }

    @Test
    void getOrganizationByIdShouldRejectMissingOrganization() {
        when(organizationRepository.findById(404L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.getOrganizationById(404L)
        );

        assertEquals(
                "Organization not found",
                exception.getMessage()
        );
    }

    @Test
    void updateCurrentOrganizationShouldUseTenantContext() {
        User creator = activeUser(1L);
        Organization organization = organization(
                10L,
                creator,
                2L
        );

        TenantContextHolder.set(new TenantContext(
                10L,
                100L,
                1L,
                RoleName.OWNER,
                DataScope.ALL,
                null,
                Set.of()
        ));

        when(organizationRepository.findById(10L))
                .thenReturn(Optional.of(organization));
        when(organizationRepository.saveAndFlush(organization))
                .thenReturn(organization);

        OrganizationResponse response = organizationService
                .updateCurrentOrganization(
                        new UpdateOrganizationRequest(
                                "Tadamun Group",
                                "UTC",
                                2L
                        )
                );

        assertEquals("Tadamun Group", response.name());
        assertEquals("UTC", response.timeZone());
        verify(auditLogService).log(
                eq(1L),
                eq("ORGANIZATION_UPDATED"),
                eq("ORGANIZATION"),
                eq(10L),
                anyString()
        );
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("System Administrator");
        user.setEmail("admin@crm.com");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Organization organization(
            Long id,
            User creator,
            Long version
    ) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Tadamun Business");
        organization.setSlug("tadamun-business");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(creator);
        organization.setVersion(version);
        return organization;
    }
}
