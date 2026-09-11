package com.crm.backend.integration.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationDeliveryAttemptRepository
        extends JpaRepository<IntegrationDeliveryAttempt, Long> {

    List<IntegrationDeliveryAttempt>
    findByOrganizationIdAndDeliveryIdOrderByAttemptNumberAsc(
            Long organizationId,
            Long deliveryId
    );
}
