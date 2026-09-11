package com.crm.backend.integration.delivery;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IntegrationDeliveryRepository
        extends JpaRepository<IntegrationDelivery, Long> {

    @EntityGraph(attributePaths = {"connection", "createdByUser"})
    Page<IntegrationDelivery>
    findByOrganizationIdAndConnectionIdOrderByCreatedAtDesc(
            Long organizationId,
            Long connectionId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"connection", "createdByUser"})
    Optional<IntegrationDelivery> findByPublicDeliveryIdAndOrganizationId(
            String publicDeliveryId,
            Long organizationId
    );

    Optional<IntegrationDelivery> findByOrganizationIdAndIdempotencyKey(
            Long organizationId,
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT delivery
            FROM IntegrationDelivery delivery
            JOIN FETCH delivery.organization
            JOIN FETCH delivery.connection
            WHERE delivery.id = :id
            """)
    Optional<IntegrationDelivery> findForWorkerUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT *
            FROM integration_deliveries
            WHERE status IN ('PENDING', 'RETRY_SCHEDULED')
              AND next_attempt_at <= :now
              AND (claimed_at IS NULL OR claimed_at < :staleBefore)
            ORDER BY next_attempt_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<IntegrationDelivery> lockReadyForDelivery(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("batchSize") int batchSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE integration_deliveries
            SET status = CASE
                    WHEN attempt_count >= maximum_attempts THEN 'DEAD'
                    ELSE 'RETRY_SCHEDULED'
                END,
                next_attempt_at = :now,
                claimed_at = NULL,
                claim_token = NULL,
                last_error_category = 'STALE_CLAIM',
                last_error_message = 'Recovered an abandoned delivery',
                completed_at = CASE
                    WHEN attempt_count >= maximum_attempts THEN :now
                    ELSE NULL
                END
            WHERE status = 'PROCESSING'
              AND claimed_at < :staleBefore
            """, nativeQuery = true)
    int recoverStaleProcessing(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore
    );
}
