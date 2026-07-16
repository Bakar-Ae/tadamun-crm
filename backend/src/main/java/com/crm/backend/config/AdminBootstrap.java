package com.crm.backend.config;

import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:admin@crm.com}") String adminEmail,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword.isBlank()) {
            return;
        }

        if (adminPassword.length() < 12) {
            throw new IllegalStateException("Bootstrap administrator password must contain at least 12 characters");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Bootstrap administrator account was not found"));

        if (!admin.isPasswordChangeRequired()) {
            log.info("Administrator bootstrap skipped because setup is already complete");
            return;
        }

        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        userRepository.save(admin);
        log.info("Bootstrap administrator credential initialized for first login");
    }
}
