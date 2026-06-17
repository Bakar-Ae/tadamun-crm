package com.crm.backend.auth;

import com.crm.backend.email.EmailService;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public PasswordResetService(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            EmailService emailService,
            @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
                .ifPresent(user -> {
                    revokeActiveResetTokens(user);
                    String rawToken = generateRawToken();
                    String tokenHash = hashToken(rawToken);

                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setTokenHash(tokenHash);
                    resetToken.setUser(user);
                    resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
                    resetToken.setUsed(false);

                    passwordResetTokenRepository.save(resetToken);

                    String resetLink = frontendBaseUrl + "/reset-password?token=" + rawToken;
                    emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        String tokenHash = hashToken(request.token());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());

        refreshTokenService.revokeAllRefreshTokensForUser(user.getId());
    }
    private void revokeActiveResetTokens(User user) {
        passwordResetTokenRepository.findByUserAndUsedFalse(user)
                .forEach(token -> {
                    token.setUsed(true);
                    token.setUsedAt(LocalDateTime.now());
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash password reset token", exception);
        }
    }
}