package com.crm.backend.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

public final class RequestObservabilityContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String ORGANIZATION_ID_MDC_KEY = "organizationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String ORGANIZATION_ID_ATTRIBUTE =
            RequestObservabilityContext.class.getName() + ".organizationId";
    public static final String USER_ID_ATTRIBUTE =
            RequestObservabilityContext.class.getName() + ".userId";

    private RequestObservabilityContext() {
    }

    public static void bindTenant(
            HttpServletRequest request,
            Long organizationId,
            Long userId
    ) {
        if (organizationId != null) {
            request.setAttribute(ORGANIZATION_ID_ATTRIBUTE, organizationId);
            MDC.put(
                    ORGANIZATION_ID_MDC_KEY,
                    organizationId.toString()
            );
        }
        if (userId != null) {
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
            MDC.put(USER_ID_MDC_KEY, userId.toString());
        }
    }

    public static void restoreTenant(HttpServletRequest request) {
        putAttributeInMdc(
                request,
                ORGANIZATION_ID_ATTRIBUTE,
                ORGANIZATION_ID_MDC_KEY
        );
        putAttributeInMdc(
                request,
                USER_ID_ATTRIBUTE,
                USER_ID_MDC_KEY
        );
    }

    public static boolean hasTenant(HttpServletRequest request) {
        return request.getAttribute(ORGANIZATION_ID_ATTRIBUTE) != null;
    }

    public static void clearTenant() {
        MDC.remove(ORGANIZATION_ID_MDC_KEY);
        MDC.remove(USER_ID_MDC_KEY);
    }

    private static void putAttributeInMdc(
            HttpServletRequest request,
            String attributeName,
            String mdcKey
    ) {
        Object value = request.getAttribute(attributeName);
        if (value != null) {
            MDC.put(mdcKey, value.toString());
        }
    }
}
