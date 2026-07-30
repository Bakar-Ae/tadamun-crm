package com.crm.backend.security.tenant;

import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantResolutionFilterTest {

    private TenantRoutePolicy routePolicy;
    private TenantResolutionService resolutionService;
    private TenantResolutionFilter filter;
    private CustomUserDetails userDetails;
    private Authentication originalAuthentication;

    @BeforeEach
    void setUp() {
        routePolicy = mock(TenantRoutePolicy.class);
        resolutionService = mock(TenantResolutionService.class);
        filter = new TenantResolutionFilter(
                routePolicy,
                resolutionService,
                true
        );

        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(20L);

        originalAuthentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(originalAuthentication);
    }

    @AfterEach
    void clearSecurityContext() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTenantShouldInstallAndClearContext() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/customers"
                );
        request.addHeader(
                TenantResolutionFilter.ORGANIZATION_HEADER,
                "10"
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        TenantContext context = context();
        when(routePolicy.requiresTenantContext(request))
                .thenReturn(true);
        when(resolutionService.resolve(20L, 10L))
                .thenReturn(context);

        AtomicReference<TenantContext> observedContext =
                new AtomicReference<>();
        AtomicReference<Authentication> observedAuthentication =
                new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) -> {
            observedContext.set(TenantContextHolder.getRequired());
            observedAuthentication.set(
                    SecurityContextHolder.getContext()
                            .getAuthentication()
            );
        };

        filter.doFilter(request, response, chain);

        assertSame(context, observedContext.get());
        assertTrue(
                observedAuthentication.get().getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("CUSTOMER_VIEW")
                        )
        );
        assertEquals(
                "10",
                response.getHeader(
                        TenantResolutionFilter.ORGANIZATION_HEADER
                )
        );
        assertTrue(TenantContextHolder.getOptional().isEmpty());
        assertSame(
                originalAuthentication,
                SecurityContextHolder.getContext()
                        .getAuthentication()
        );
    }

    @Test
    void malformedOrganizationHeaderShouldReturnBadRequest()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/customers"
                );
        request.addHeader(
                TenantResolutionFilter.ORGANIZATION_HEADER,
                "not-a-number"
        );
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(routePolicy.requiresTenantContext(request))
                .thenReturn(true);

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        fail("Filter chain must not continue")
        );

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains(
                "\"code\":\"INVALID_ORGANIZATION_ID\""
        ));
        verifyNoInteractions(resolutionService);
        assertTrue(TenantContextHolder.getOptional().isEmpty());
    }

    @Test
    void missingHeaderShouldUseSingleMembershipResolution()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/customers"
                );
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(routePolicy.requiresTenantContext(request))
                .thenReturn(true);
        when(resolutionService.resolve(20L, null))
                .thenReturn(context());

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                }
        );

        verify(resolutionService).resolve(20L, null);
        assertEquals(
                "10",
                response.getHeader(
                        TenantResolutionFilter.ORGANIZATION_HEADER
                )
        );
    }

    private TenantContext context() {
        return new TenantContext(
                10L,
                50L,
                20L,
                RoleName.MANAGER,
                DataScope.TEAM,
                null,
                Set.of(PermissionName.CUSTOMER_VIEW)
        );
    }
}
