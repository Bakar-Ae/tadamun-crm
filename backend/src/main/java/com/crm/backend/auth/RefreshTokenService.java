package com.crm.backend.auth;

import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.security.JwtService;
import com.crm.backend.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;
    private final JwtService jwtService;
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
    }

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshAccessToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        CustomUserDetails userDetails = new CustomUserDetails(refreshToken.getUser());
        String accessToken = jwtService.generateToken(userDetails);

        return new RefreshTokenResponse(accessToken, "Bearer");
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}