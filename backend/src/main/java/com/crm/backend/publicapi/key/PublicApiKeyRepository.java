package com.crm.backend.publicapi.key;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PublicApiKeyRepository
        extends JpaRepository<PublicApiKey, Long> {

    @EntityGraph(attributePaths = {"organization", "scopes"})
    Optional<PublicApiKey> findByPublicId(String publicId);

    @EntityGraph(attributePaths = {
            "scopes",
            "createdByUser",
            "revokedByUser"
    })
    Optional<PublicApiKey> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "scopes",
            "createdByUser",
            "revokedByUser"
    })
    @Query("""
            SELECT apiKey FROM PublicApiKey apiKey
            WHERE apiKey.id = :id
              AND apiKey.organization.id = :organizationId
            """)
    Optional<PublicApiKey> findForUpdate(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId
    );

    Page<PublicApiKey> findByOrganizationId(
            Long organizationId,
            Pageable pageable
    );

    boolean existsByPublicId(String publicId);
}
