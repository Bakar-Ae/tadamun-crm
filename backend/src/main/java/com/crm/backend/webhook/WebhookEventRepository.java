package com.crm.backend.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface WebhookEventRepository
        extends JpaRepository<WebhookEvent, Long> {

    boolean existsByPublicEventId(String publicEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event FROM WebhookEvent event
            WHERE event.id = :id
            """)
    Optional<WebhookEvent> findForPublicationUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT *
            FROM webhook_events
            WHERE publication_status = 'PENDING'
              AND next_publication_attempt_at <= :now
              AND (claimed_at IS NULL OR claimed_at < :staleBefore)
            ORDER BY next_publication_attempt_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<WebhookEvent> lockReadyForPublication(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("batchSize") int batchSize
    );
}
