package com.crm.backend.security.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantRoutePolicyTest {

    private final TenantRoutePolicy routePolicy =
            new TenantRoutePolicy();

    @Test
    void crmRoutesShouldRequireTenantContext() {
        assertTrue(requiresTenant("GET", "/api/v1/customers"));
        assertTrue(requiresTenant("POST", "/api/v1/leads"));
        assertTrue(requiresTenant("GET", "/api/v1/reports/summary"));
        assertTrue(requiresTenant("GET", "/api/v1/organization"));
        assertTrue(requiresTenant("GET", "/api/v1/organization/members"));
    }

    @Test
    void publicAndGlobalRoutesShouldNotRequireTenantContext() {
        assertFalse(requiresTenant("POST", "/api/v1/auth/login"));
        assertFalse(requiresTenant("GET", "/api/v1/auth/me"));
        assertFalse(requiresTenant("GET", "/actuator/health"));
        assertFalse(requiresTenant("GET", "/api/v1/platform/status"));
    }

    @Test
    void optionsAndSimilarPrefixesShouldNotRequireTenantContext() {
        assertFalse(requiresTenant("OPTIONS", "/api/v1/customers"));
        assertFalse(requiresTenant("GET", "/api/v1/customerships"));
    }

    private boolean requiresTenant(String method, String path) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);

        return routePolicy.requiresTenantContext(request);
    }
}
