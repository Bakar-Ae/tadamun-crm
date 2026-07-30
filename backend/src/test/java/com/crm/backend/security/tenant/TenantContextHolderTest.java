package com.crm.backend.security.tenant;

import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextHolderTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void contextShouldBeAvailableAndCleared() {
        TenantContext context = context();

        TenantContextHolder.set(context);

        assertSame(context, TenantContextHolder.getRequired());
        assertEquals(context, TenantContextHolder.getOptional().orElseThrow());

        TenantContextHolder.clear();

        assertTrue(TenantContextHolder.getOptional().isEmpty());
    }

    @Test
    void requiredContextShouldFailWhenMissing() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                TenantContextHolder::getRequired
        );

        assertEquals(
                "Tenant context is required for this operation",
                exception.getMessage()
        );
    }

    private TenantContext context() {
        return new TenantContext(
                10L,
                20L,
                30L,
                RoleName.ADMIN,
                DataScope.ALL,
                null,
                Set.of(PermissionName.CUSTOMER_VIEW)
        );
    }
}
