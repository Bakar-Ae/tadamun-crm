package com.crm.backend.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationCredentialRepository
        extends JpaRepository<IntegrationCredential, Long> {

    Optional<IntegrationCredential> findByConnectionIdAndOrganizationId(
            Long connectionId,
            Long organizationId
    );

    boolean existsByConnectionIdAndOrganizationId(
            Long connectionId,
            Long organizationId
    );
}
