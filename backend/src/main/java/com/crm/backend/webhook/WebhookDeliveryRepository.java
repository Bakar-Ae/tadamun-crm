package com.crm.backend.webhook;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface WebhookDeliveryRepository
        extends JpaRepository<WebhookDelivery, Long> {

    Page<WebhookDelivery>
    findByOrganizationIdAndSubscriptionIdOrderByCreatedAtDesc(
            Long organizationId, Long subscriptionId, Pageable pageable);

    Optional<WebhookDelivery> findByPublicDeliveryIdAndOrganizationIdAndSubscriptionId(
            String publicDeliveryId,
            Long organizationId,
            Long subscriptionId
    );

    boolean existsByEventIdAndSubscriptionId(
            Long eventId, Long subscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT delivery FROM WebhookDelivery delivery
            JOIN FETCH delivery.organization
            JOIN FETCH delivery.event
            JOIN FETCH delivery.subscription
            WHERE delivery.publicDeliveryId = :publicDeliveryId
              AND delivery.organization.id = :organizationId
              AND delivery.subscription.id = :subscriptionId
            """)
    Optional<WebhookDelivery> findForReplay(
            @Param("publicDeliveryId") String publicDeliveryId,
            @Param("organizationId") Long organizationId,
            @Param("subscriptionId") Long subscriptionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT delivery FROM WebhookDelivery delivery
            JOIN FETCH delivery.organization
            JOIN FETCH delivery.event
            JOIN FETCH delivery.subscription
            WHERE delivery.id = :id
            """)
    Optional<WebhookDelivery> findForWorkerUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT *
            FROM webhook_deliveries
            WHERE status IN ('PENDING', 'RETRY_SCHEDULED')
              AND next_attempt_at <= :now
              AND (claimed_at IS NULL OR claimed_at < :staleBefore)
            ORDER BY next_attempt_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<WebhookDelivery> lockReadyForDelivery(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("batchSize") int batchSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE webhook_deliveries
            SET status = CASE
                    WHEN attempt_count >= :maximumAttempts THEN 'DEAD'
                    ELSE 'RETRY_SCHEDULED'
                END,
                next_attempt_at = :now,
                claimed_at = NULL,
                claim_token = NULL,
                last_error_category = 'STALE_CLAIM',
                last_error = 'Recovered an abandoned webhook attempt',
                completed_at = CASE
                    WHEN attempt_count >= :maximumAttempts THEN :now
                    ELSE NULL
                END
            WHERE status = 'PROCESSING'
              AND claimed_at < :staleBefore
            """, nativeQuery = true)
    int recoverStaleProcessing(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maximumAttempts") int maximumAttempts
    );
}
