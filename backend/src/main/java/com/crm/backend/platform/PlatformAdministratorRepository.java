package com.crm.backend.platform;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAdministratorRepository
        extends JpaRepository<PlatformAdministrator, Long> {

    boolean existsByUserIdAndStatus(
            Long userId,
            PlatformAdministratorStatus status
    );
}
