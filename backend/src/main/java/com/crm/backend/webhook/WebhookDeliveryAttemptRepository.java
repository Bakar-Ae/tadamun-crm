package com.crm.backend.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryAttemptRepository
        extends JpaRepository<WebhookDeliveryAttempt, Long> {

    List<WebhookDeliveryAttempt>
    findByOrganizationIdAndDeliveryIdOrderByAttemptNumberAsc(
            Long organizationId, Long deliveryId);
}