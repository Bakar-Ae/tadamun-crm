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

    public AuthService(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException exception) {
            log.warn("Login failed for email {}", request.email());
            throw exception;
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(request.email());

        String token = jwtService.generateToken(userDetails);

        log.info("Login successful for user {}", userDetails.getUsername());

        return buildLoginResponse(token, userDetails);
    }

    public LoginResponse me(CustomUserDetails userDetails) {
        return buildLoginResponse(null, userDetails);
    }

    private LoginResponse buildLoginResponse(String token, CustomUserDetails userDetails) {
        return new LoginResponse(
                token,
                token == null ? null : "Bearer",
                userDetails.getId(),
                userDetails.getFullName(),
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        );
    }
}