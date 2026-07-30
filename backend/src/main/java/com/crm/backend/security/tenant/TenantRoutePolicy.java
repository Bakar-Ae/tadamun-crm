package com.crm.backend.security.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantRoutePolicy {

    private static final List<String> TENANT_PREFIXES = List.of(
            "/api/v1/activities",
            "/api/v1/attachments",
            "/api/v1/audit-logs",
            "/api/v1/contacts",
            "/api/v1/customers",
            "/api/v1/dashboard",
            "/api/v1/leads",
            "/api/v1/notes",
            "/api/v1/notifications",
            "/api/v1/permissions",
            "/api/v1/reports",
            "/api/v1/roles",
            "/api/v1/search",
            "/api/v1/tasks",
            "/api/v1/users"
    );

    public boolean requiresTenantContext(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return TENANT_PREFIXES.stream()
                .anyMatch(prefix ->
                        path.equals(prefix)
                                || path.startsWith(prefix + "/")
                );
    }
}
