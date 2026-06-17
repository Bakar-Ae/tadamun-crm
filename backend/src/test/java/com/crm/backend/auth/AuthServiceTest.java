package com.crm.backend.auth;

import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.security.CustomUserDetailsService;
import com.crm.backend.security.JwtService;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private CustomUserDetailsService customUserDetailsService;
    private JwtService jwtService;
    private LoginAttemptService loginAttemptService;
    private RefreshTokenService refreshTokenService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        customUserDetailsService = mock(CustomUserDetailsService.class);
        jwtService = mock(JwtService.class);
        loginAttemptService = new LoginAttemptService();
        refreshTokenService = mock(RefreshTokenService.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authService = new AuthService(
                authenticationManager,
                customUserDetailsService,
                jwtService,
                loginAttemptService,
                refreshTokenService,
                userRepository,
                passwordEncoder
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
        user.setPasswordChangeRequired(true);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("admin@crm.com"))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("fake-jwt-token");

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn("fake-refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("fake-jwt-token", response.accessToken());
        assertEquals("fake-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1L, response.userId());
        assertEquals("admin@crm.com", response.email());
        assertEquals("ROLE_ADMIN", response.role());
        assertEquals(true, response.passwordChangeRequired());
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

    @Test
    void loginShouldBlockAfterTooManyFailedAttempts() {
        LoginRequest request = new LoginRequest(
                "admin@crm.com",
                "wrong-password"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        for (int i = 0; i < 5; i++) {
            assertThrows(
                    BadCredentialsException.class,
                    () -> authService.login(request)
            );
        }

        TooManyLoginAttemptsException exception = assertThrows(
                TooManyLoginAttemptsException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Too many failed login attempts. Please try again later.",
                exception.getMessage()
        );
    }

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        User user = createAdminUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword1",
                "NewPassword123",
                "NewPassword123"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1", "hashed-password"))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(userDetails, request)
        );

        assertEquals("Current password is incorrect", exception.getMessage());
    }

    @Test
    void changePasswordShouldRejectMismatchedConfirmation() {
        User user = createAdminUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "Admin@12345",
                "NewPassword123",
                "DifferentPassword123"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(userDetails, request)
        );

        assertEquals("New password and confirmation do not match", exception.getMessage());
    }
    @Test
    void changePasswordShouldUpdatePasswordAndClearRequiredFlag() {
        User user = createAdminUser();
        user.setPasswordChangeRequired(true);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "Admin@12345",
                "NewPassword123",
                "NewPassword123"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@12345", "hashed-password"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewPassword123", "hashed-password"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("new-hashed-password");

        authService.changePassword(userDetails, request);

        assertEquals("new-hashed-password", user.getPasswordHash());
        assertEquals(false, user.isPasswordChangeRequired());
    }

    private User createAdminUser() {
        Role role = new Role();
        role.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(1L);
        user.setFullName("System Administrator");
        user.setEmail("admin@crm.com");
        user.setPasswordHash("hashed-password");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }
}