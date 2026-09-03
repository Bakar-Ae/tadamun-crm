package com.crm.backend.security;

import com.crm.backend.role.DataScope;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.user.User;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataScopeServiceTest {

    private final DataScopeService service = new DataScopeService();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantMembershipScopeShouldOverrideLegacyGlobalRoleScope() {
        authenticate(20L, DataScope.ALL);
        TenantContextHolder.set(new TenantContext(
                10L,
                50L,
                20L,
                RoleName.SALES_REP,
                DataScope.OWN,
                99L,
                Set.of()
        ));

        DataScopeContext context = service.currentContext();

        assertEquals(20L, context.userId());
        assertEquals(99L, context.teamId());
        assertEquals(DataScope.OWN, context.scope());
    }

    @Test
    void contextForAnotherUserShouldBeRejected() {
        authenticate(20L, DataScope.ALL);
        TenantContextHolder.set(new TenantContext(
                10L,
                50L,
                21L,
                RoleName.MANAGER,
                DataScope.TEAM,
                null,
                Set.of()
        ));

        assertThrows(
                AccessDeniedException.class,
                service::currentContext
        );
    }

    private void authenticate(Long userId, DataScope globalScope) {
        Role globalRole = new Role();
        globalRole.setName(RoleName.ADMIN);
        globalRole.setDataScope(globalScope);
        globalRole.setPermissions(Set.of());

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(globalRole);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }
}
