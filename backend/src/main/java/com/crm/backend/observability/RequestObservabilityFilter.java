package com.crm.backend.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestObservabilityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
            RequestObservabilityFilter.class
    );
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile(
            "[A-Za-z0-9._-]{8,100}"
    );
    private static final String UNMATCHED_ROUTE = "unmatched";

    private final MeterRegistry meterRegistry;

    public RequestObservabilityFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = requestId(request);
        long startedAt = System.nanoTime();
        boolean failed = false;

        RequestObservabilityContext.clearTenant();
        MDC.put(RequestObservabilityContext.REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(
                RequestObservabilityContext.REQUEST_ID_HEADER,
                requestId
        );

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            long durationNanos = System.nanoTime() - startedAt;
            int status = failed && response.getStatus() < 500
                    ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    : response.getStatus();

            try {
                RequestObservabilityContext.restoreTenant(request);
                recordRequest(request, status, durationNanos);
                logCompletion(request, status, durationNanos);
            } finally {
                RequestObservabilityContext.clearTenant();
                MDC.remove(RequestObservabilityContext.REQUEST_ID_MDC_KEY);
            }
        }
    }

    private String requestId(HttpServletRequest request) {
        String supplied = request.getHeader(
                RequestObservabilityContext.REQUEST_ID_HEADER
        );
        if (supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return UUID.randomUUID().toString();
    }

    private void recordRequest(
            HttpServletRequest request,
            int status,
            long durationNanos
    ) {
        try {
            Timer.builder("crm.http.server.requests")
                    .description("Tadamun CRM HTTP request duration")
                    .tag("method", request.getMethod())
                    .tag("route", route(request))
                    .tag("status", Integer.toString(status))
                    .tag(
                            "tenant",
                            RequestObservabilityContext.hasTenant(request)
                                    ? "present"
                                    : "absent"
                    )
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
        } catch (RuntimeException metricFailure) {
            log.warn(
                    "Request metric recording failed. reason={}",
                    metricFailure.getClass().getSimpleName()
            );
        }
    }

    private void logCompletion(
            HttpServletRequest request,
            int status,
            long durationNanos
    ) {
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);
        if (status >= 500) {
            log.warn(
                    "Request completed. method={}, route={}, status={}, durationMs={}",
                    request.getMethod(),
                    route(request),
                    status,
                    durationMillis
            );
            return;
        }
        log.info(
                "Request completed. method={}, route={}, status={}, durationMs={}",
                request.getMethod(),
                route(request),
                status,
                durationMillis
        );
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );
        return pattern instanceof String route && !route.isBlank()
                ? route
                : UNMATCHED_ROUTE;
    }
}
