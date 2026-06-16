package com.crm.backend.auth;

import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.security.CustomUserDetailsService;
import com.crm.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            LoginAttemptService loginAttemptService,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        loginAttemptService.checkBlocked(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException exception) {
            loginAttemptService.loginFailed(request.email());
            log.warn("Login failed for email {}", request.email());
            throw exception;
        }

        loginAttemptService.loginSucceeded(request.email());

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(request.email());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());

        log.info("Login successful for user {}", userDetails.getUsername());

        return buildLoginResponse(accessToken, refreshToken, userDetails);
    }

    public LoginResponse me(CustomUserDetails userDetails) {
        return buildLoginResponse(null, null, userDetails);
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, CustomUserDetails userDetails) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                accessToken == null ? null : "Bearer",
                userDetails.getId(),
                userDetails.getFullName(),
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        );
    }
}