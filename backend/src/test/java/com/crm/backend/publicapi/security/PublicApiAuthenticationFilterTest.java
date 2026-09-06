package com.crm.backend.publicapi.security;

import com.crm.backend.publicapi.PublicApiAuditService;
import com.crm.backend.publicapi.key.PublicApiScope;
import com.crm.backend.subscription.SubscriptionFeatureUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicApiAuthenticationFilterTest {

    @Test
    void missingCredentialShouldReturnSanitizedUnauthorizedResponse()
            throws Exception {
        PublicApiAuthenticationService authenticationService =
                mock(PublicApiAuthenticationService.class);
        PublicApiAuditService auditService = mock(PublicApiAuditService.class);
        PublicApiAuthenticationFilter filter =
                new PublicApiAuthenticationFilter(
                        authenticationService,
                        new PublicApiRateLimiter(),
                        auditService
                );
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("INVALID_API_KEY"));
        verify(auditService).logPlatformAuthenticationFailure();
    }

    @Test
    void disabledSubscriptionShouldReturnUpgradeRequiredResponse()
            throws Exception {
        PublicApiAuthenticationService authenticationService =
                mock(PublicApiAuthenticationService.class);
        when(authenticationService.authenticate("test-key"))
                .thenThrow(new SubscriptionFeatureUnavailableException(
                        com.crm.backend.subscription.SubscriptionFeature.PUBLIC_API
                ));
        PublicApiAuthenticationFilter filter =
                new PublicApiAuthenticationFilter(
                        authenticationService,
                        new PublicApiRateLimiter(),
                        mock(PublicApiAuditService.class)
                );
        MockHttpServletRequest request = request();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(402, response.getStatus());
        assertTrue(response.getContentAsString().contains(
                "SUBSCRIPTION_FEATURE_REQUIRED"
        ));
    }

    @Test
    void exceededLimitShouldReturnRetryAfterHeader() throws Exception {
        PublicApiAuthenticationService authenticationService =
                mock(PublicApiAuthenticationService.class);
        PublicApiPrincipal principal = new PublicApiPrincipal(
                5L,
                42L,
                "Integration",
                Set.of(PublicApiScope.CUSTOMERS_READ),
                1
        );
        when(authenticationService.authenticate("test-key"))
                .thenReturn(principal);
        PublicApiAuthenticationFilter filter =
                new PublicApiAuthenticationFilter(
                        authenticationService,
                        new PublicApiRateLimiter(),
                        mock(PublicApiAuditService.class)
                );

        MockHttpServletRequest firstRequest = request();
        firstRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        filter.doFilter(
                firstRequest,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        MockHttpServletRequest secondRequest = request();
        secondRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(
                secondRequest,
                secondResponse,
                new MockFilterChain()
        );

        assertEquals(429, secondResponse.getStatus());
        assertTrue(Integer.parseInt(
                secondResponse.getHeader(HttpHeaders.RETRY_AFTER)
        ) > 0);
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest(
                "GET",
                "/api/public/v1/customers"
        );
    }
}
