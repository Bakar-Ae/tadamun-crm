package com.crm.backend.publicapi.security;

import com.crm.backend.publicapi.PublicApiAuditAction;
import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.subscription.SubscriptionFeatureUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class PublicApiAuthenticationFilter extends OncePerRequestFilter {

    public static final String PUBLIC_API_PREFIX = "/api/public/v1";
    private static final Logger LOGGER = LoggerFactory.getLogger(
            PublicApiAuthenticationFilter.class
    );

    private final PublicApiAuthenticationService authenticationService;
    private final PublicApiRateLimiter rateLimiter;
    private final PublicApiAuditService auditService;

    public PublicApiAuthenticationFilter(
            PublicApiAuthenticationService authenticationService,
            PublicApiRateLimiter rateLimiter,
            PublicApiAuditService auditService
    ) {
        this.authenticationService = authenticationService;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !(path.equals(PUBLIC_API_PREFIX)
                || path.startsWith(PUBLIC_API_PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        PublicApiContextHolder.clear();
        String rawKey = bearerToken(request);

        if (rawKey == null) {
            auditMissingCredential();
            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "INVALID_API_KEY",
                    "A valid API key is required"
            );
            return;
        }

        PublicApiPrincipal principal;
        try {
            principal = authenticationService.authenticate(rawKey);
        } catch (PublicApiAuthenticationException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "INVALID_API_KEY",
                    "A valid API key is required"
            );
            return;
        } catch (SubscriptionFeatureUnavailableException exception) {
            writeError(
                    response,
                    HttpServletResponse.SC_PAYMENT_REQUIRED,
                    "Payment Required",
                    "SUBSCRIPTION_FEATURE_REQUIRED",
                    "Current subscription does not include public API access"
            );
            return;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected public API authentication failure", exception);
            writeError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    "PUBLIC_API_ERROR",
                    "Unexpected server error"
            );
            return;
        }

        PublicApiRateLimiter.Decision decision = rateLimiter.tryAcquire(
                principal.apiKeyId(),
                principal.rateLimitPerMinute()
        );

        if (!decision.allowed()) {
            auditRateLimit(principal);
            response.setHeader(
                    HttpHeaders.RETRY_AFTER,
                    Integer.toString(decision.retryAfterSeconds())
            );
            writeError(
                    response,
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "RATE_LIMIT_EXCEEDED",
                    "Public API rate limit exceeded"
            );
            return;
        }

        PublicApiContextHolder.set(principal);
        installAuthentication(request, principal);

        try {
            filterChain.doFilter(request, response);
        } finally {
            PublicApiContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        String token = header.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private void installAuthentication(
            HttpServletRequest request,
            PublicApiPrincipal principal
    ) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.authorities()
                );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void auditMissingCredential() {
        try {
            auditService.logPlatformAuthenticationFailure();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist missing API credential audit");
        }
    }

    private void auditRateLimit(PublicApiPrincipal principal) {
        try {
            auditService.logSecurityEventForOrganization(
                    principal.organizationId(),
                    PublicApiAuditAction.PUBLIC_API_RATE_LIMIT_EXCEEDED,
                    principal.apiKeyId(),
                    auditService.details("result", "rejected")
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not persist public API rate-limit audit");
        }
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String error,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"error":"%s","code":"%s","message":"%s"}
                """.formatted(
                Instant.now(),
                status,
                error,
                code,
                message
        ).trim());
    }
}
