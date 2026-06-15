package com.crm.backend.auth;

import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.security.CustomUserDetailsService;
import com.crm.backend.security.JwtService;
import com.crm.backend.user.User;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private CustomUserDetailsService customUserDetailsService;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        customUserDetailsService = mock(CustomUserDetailsService.class);
        jwtService = mock(JwtService.class);

        authService = new AuthService(
                authenticationManager,
                customUserDetailsService,
                jwtService
        );
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() {
        LoginRequest request = new LoginRequest(
                "admin@crm.com",
                "Admin@12345"
        );

        Role role = new Role();
        role.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(1L);
        user.setFullName("System Administrator");
        user.setEmail("admin@crm.com");
        user.setPasswordHash("hashed-password");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("admin@crm.com"))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("fake-jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1L, response.userId());
        assertEquals("admin@crm.com", response.email());
        assertEquals("ROLE_ADMIN", response.role());
    }

    @Test
    void loginShouldRejectBadCredentials() {
        LoginRequest request = new LoginRequest(
                "admin@crm.com",
                "wrong-password"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );
    }
}