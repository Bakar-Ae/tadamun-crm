package com.crm.backend.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestObservabilityFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void requestShouldCarryCorrelationAndRecordTenantSafeMetric()
            throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RequestObservabilityFilter filter =
                new RequestObservabilityFilter(registry);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/customers/42"
        );
        request.addHeader(
                RequestObservabilityContext.REQUEST_ID_HEADER,
                "request-12345"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertEquals(
                    "request-12345",
                    MDC.get(RequestObservabilityContext.REQUEST_ID_MDC_KEY)
            );
            HttpServletRequest httpRequest =
                    (HttpServletRequest) servletRequest;
            RequestObservabilityContext.bindTenant(httpRequest, 9L, 7L);
            httpRequest.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/v1/customers/{customerId}"
            );
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        assertEquals(
                "request-12345",
                response.getHeader(
                        RequestObservabilityContext.REQUEST_ID_HEADER
                )
        );
        Timer timer = registry.find("crm.http.server.requests")
                .tags(
                        "method", "GET",
                        "route", "/api/v1/customers/{customerId}",
                        "status", "200",
                        "tenant", "present"
                )
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
        assertNull(MDC.get(RequestObservabilityContext.REQUEST_ID_MDC_KEY));
        assertNull(MDC.get(RequestObservabilityContext.ORGANIZATION_ID_MDC_KEY));
        assertFalse(registry.getMeters().stream().anyMatch(meter ->
                meter.getId().getTags().stream().anyMatch(tag ->
                        tag.getValue().equals("9")
                )
        ));
    }

    @Test
    void unsafeRequestIdShouldBeReplaced() throws Exception {
        RequestObservabilityFilter filter =
                new RequestObservabilityFilter(new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/actuator/health"
        );
        request.addHeader(
                RequestObservabilityContext.REQUEST_ID_HEADER,
                "bad value with spaces"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) -> {
                }
        );

        String requestId = response.getHeader(
                RequestObservabilityContext.REQUEST_ID_HEADER
        );
        assertNotNull(requestId);
        assertNotEquals("bad value with spaces", requestId);
    }
}
