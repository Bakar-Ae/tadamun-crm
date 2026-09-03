package com.crm.backend.security;

import com.crm.backend.platform.PlatformAdministratorRepository;
import com.crm.backend.platform.PlatformAdministratorStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private PlatformAdministratorRepository platformAdministratorRepository;
    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        platformAdministratorRepository =
                mock(PlatformAdministratorRepository.class);
        service = new CustomUserDetailsService(
                userRepository,
                platformAdministratorRepository
        );
    }

    @Test
    void explicitActivePlatformAdministratorShouldReceivePlatformAuthority() {
        User user = user(RoleName.SALES_REP);
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(platformAdministratorRepository.existsByUserIdAndStatus(
                user.getId(),
                PlatformAdministratorStatus.ACTIVE
        )).thenReturn(true);

        CustomUserDetails details = (CustomUserDetails)
                service.loadUserByUsername(user.getEmail());

        assertTrue(hasAuthority(details, PlatformAuthorities.ADMIN));
        assertTrue(details.isPlatformAdministrator());
    }

    @Test
    void legacyAdminRoleShouldNotImplyPlatformAuthority() {
        User user = user(RoleName.ADMIN);
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(platformAdministratorRepository.existsByUserIdAndStatus(
                user.getId(),
                PlatformAdministratorStatus.ACTIVE
        )).thenReturn(false);

        CustomUserDetails details = (CustomUserDetails)
                service.loadUserByUsername(user.getEmail());

        assertFalse(hasAuthority(details, PlatformAuthorities.ADMIN));
        assertFalse(details.isPlatformAdministrator());
    }

    private boolean hasAuthority(
            CustomUserDetails details,
            String authority
    ) {
        return details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private User user(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setDataScope(DataScope.ALL);
        role.setPermissions(Set.of());

        User user = new User();
        user.setId(20L);
        user.setFullName("Platform Test User");
        user.setEmail("platform.test@crm.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return user;
    }
}
