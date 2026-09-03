package com.crm.backend.security;

import com.crm.backend.platform.PlatformAdministratorRepository;
import com.crm.backend.platform.PlatformAdministratorStatus;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PlatformAdministratorRepository platformAdministratorRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            PlatformAdministratorRepository platformAdministratorRepository
    ) {
        this.userRepository = userRepository;
        this.platformAdministratorRepository = platformAdministratorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        boolean platformAdministrator = platformAdministratorRepository
                .existsByUserIdAndStatus(
                        user.getId(),
                        PlatformAdministratorStatus.ACTIVE
                );

        return new CustomUserDetails(user, platformAdministrator);
    }
}
