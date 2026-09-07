package com.crm.backend.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WebhookSubscriptionEventRepository
        extends JpaRepository<WebhookSubscriptionEvent,
        WebhookSubscriptionEventId> {

    List<WebhookSubscriptionEvent>
    findByOrganizationIdAndIdSubscriptionId(
            Long organizationId, Long subscriptionId);

    List<WebhookSubscriptionEvent>
    findByOrganizationIdAndIdSubscriptionIdIn(
            Long organizationId, Collection<Long> subscriptionIds);

    @Query("""
            SELECT CASE WHEN COUNT(event) > 0 THEN true ELSE false END
            FROM WebhookSubscriptionEvent event
            WHERE event.organizationId = :organizationId
              AND event.id.eventType = :eventType
              AND event.subscription.status = :status
            """)
    boolean existsMatchingActiveSubscription(
            @Param("organizationId") Long organizationId,
            @Param("eventType") WebhookEventType eventType,
            @Param("status") WebhookSubscriptionStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM WebhookSubscriptionEvent event
            WHERE event.organizationId = :organizationId
              AND event.id.subscriptionId = :subscriptionId
            """)
    void deleteByOrganizationIdAndIdSubscriptionId(
            @Param("organizationId") Long organizationId,
            @Param("subscriptionId") Long subscriptionId);
}
