package com.crm.backend.integration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface IntegrationConnectionRepository
        extends JpaRepository<IntegrationConnection, Long> {

    @EntityGraph(attributePaths = "createdByUser")
    Page<IntegrationConnection> findByOrganizationId(
            Long organizationId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "createdByUser")
    Optional<IntegrationConnection> findByPublicConnectionIdAndOrganizationId(
            String publicConnectionId,
            Long organizationId
    );

    @EntityGraph(attributePaths = "createdByUser")
    Optional<IntegrationConnection> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    boolean existsByOrganizationIdAndProviderAndNameIgnoreCase(
            Long organizationId,
            IntegrationProvider provider,
            String name
    );

    boolean existsByOrganizationIdAndProviderAndNameIgnoreCaseAndIdNot(
            Long organizationId,
            IntegrationProvider provider,
            String name,
            Long id
    );
}
