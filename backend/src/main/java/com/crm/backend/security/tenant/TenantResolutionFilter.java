package com.crm.backend.security.tenant;

import com.crm.backend.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    public static final String ORGANIZATION_HEADER = "X-Organization-Id";

    private final TenantRoutePolicy routePolicy;
    private final TenantResolutionService resolutionService;
    private final boolean enforcementEnabled;

    public TenantResolutionFilter(
            TenantRoutePolicy routePolicy,
            TenantResolutionService resolutionService,
            @Value("${app.tenant.enforcement-enabled:true}")
            boolean enforcementEnabled
    ) {
        this.routePolicy = routePolicy;
        this.resolutionService = resolutionService;
        this.enforcementEnabled = enforcementEnabled;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        TenantContextHolder.clear();

        if (!enforcementEnabled
                || !routePolicy.requiresTenantContext(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication originalAuthentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (originalAuthentication == null
                || !originalAuthentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!(originalAuthentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {
            writeTenantError(
                    response,
                    TenantAccessException.accessDenied()
            );
            return;
        }

        try {
            Long requestedOrganizationId =
                    parseOrganizationId(request);

            TenantContext tenantContext = resolutionService.resolve(
                    userDetails.getId(),
                    requestedOrganizationId
            );

            TenantContextHolder.set(tenantContext);
            installTenantAuthentication(
                    originalAuthentication,
                    userDetails,
                    tenantContext
            );

            response.setHeader(
                    ORGANIZATION_HEADER,
                    tenantContext.organizationId().toString()
            );

            filterChain.doFilter(request, response);
        } catch (TenantAccessException exception) {
            writeTenantError(response, exception);
        } finally {
            TenantContextHolder.clear();
            SecurityContextHolder.getContext()
                    .setAuthentication(originalAuthentication);
        }
    }

    private Long parseOrganizationId(HttpServletRequest request) {
        String rawOrganizationId =
                request.getHeader(ORGANIZATION_HEADER);

        if (rawOrganizationId == null
                || rawOrganizationId.isBlank()) {
            return null;
        }

        try {
            long organizationId =
                    Long.parseLong(rawOrganizationId.trim());

            if (organizationId <= 0) {
                throw TenantAccessException.invalidOrganizationId();
            }

            return organizationId;
        } catch (NumberFormatException exception) {
            throw TenantAccessException.invalidOrganizationId();
        }
    }

    private void installTenantAuthentication(
            Authentication originalAuthentication,
            CustomUserDetails userDetails,
            TenantContext tenantContext
    ) {
        UsernamePasswordAuthenticationToken tenantAuthentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        tenantContext.authorities()
                );

        tenantAuthentication.setDetails(
                originalAuthentication.getDetails()
        );

        SecurityContextHolder.getContext()
                .setAuthentication(tenantAuthentication);
    }

    private void writeTenantError(
            HttpServletResponse response,
            TenantAccessException exception
    ) throws IOException {
        response.setStatus(exception.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = """
                {"timestamp":"%s","status":%d,"error":"%s","code":"%s","message":"%s"}
                """.formatted(
                Instant.now(),
                exception.getStatus().value(),
                escapeJson(exception.getStatus().getReasonPhrase()),
                escapeJson(exception.getErrorCode()),
                escapeJson(exception.getMessage())
        ).trim();

        response.getWriter().write(body);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
