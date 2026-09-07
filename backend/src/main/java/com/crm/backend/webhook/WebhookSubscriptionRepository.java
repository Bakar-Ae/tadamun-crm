package com.crm.backend.webhook;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscription, Long> {

    Page<WebhookSubscription> findByOrganizationId(
            Long organizationId, Pageable pageable);

    Optional<WebhookSubscription> findByIdAndOrganizationId(
            Long id, Long organizationId);

    long countByOrganizationIdAndStatusNot(
            Long organizationId, WebhookSubscriptionStatus status);

    @Query("""
            SELECT DISTINCT subscription
            FROM WebhookSubscription subscription
            JOIN WebhookSubscriptionEvent subscribedEvent
              ON subscribedEvent.subscription = subscription
            WHERE subscription.organization.id = :organizationId
              AND subscription.status = :status
              AND subscribedEvent.id.eventType = :eventType
            """)
    List<WebhookSubscription> findMatchingSubscriptions(
            @Param("organizationId") Long organizationId,
            @Param("status") WebhookSubscriptionStatus status,
            @Param("eventType") WebhookEventType eventType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT subscription FROM WebhookSubscription subscription
            WHERE subscription.id = :id
              AND subscription.organization.id = :organizationId
            """)
    Optional<WebhookSubscription> findForUpdate(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId);
}
