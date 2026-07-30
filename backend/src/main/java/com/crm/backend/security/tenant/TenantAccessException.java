package com.crm.backend.security.tenant;

import org.springframework.http.HttpStatus;

public class TenantAccessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    private TenantAccessException(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static TenantAccessException contextRequired() {
        return new TenantAccessException(
                HttpStatus.BAD_REQUEST,
                "TENANT_CONTEXT_REQUIRED",
                "Organization context is required"
        );
    }

    public static TenantAccessException invalidOrganizationId() {
        return new TenantAccessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ORGANIZATION_ID",
                "Organization ID must be a positive number"
        );
    }

    public static TenantAccessException accessDenied() {
        return new TenantAccessException(
                HttpStatus.FORBIDDEN,
                "ORGANIZATION_ACCESS_DENIED",
                "Organization access denied"
        );
    }

    public static TenantAccessException organizationUnavailable() {
        return new TenantAccessException(
                HttpStatus.FORBIDDEN,
                "ORGANIZATION_UNAVAILABLE",
                "Organization is unavailable"
        );
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
