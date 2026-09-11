package com.crm.backend.integration.delivery;

import com.crm.backend.integration.delivery.dto.IntegrationDeliveryAttemptResponse;
import com.crm.backend.integration.delivery.dto.IntegrationDeliveryResponse;
import org.springframework.stereotype.Component;

@Component
public class IntegrationDeliveryMapper {

    public IntegrationDeliveryResponse toResponse(IntegrationDelivery value) {
        return new IntegrationDeliveryResponse(
                value.getPublicDeliveryId(),
                value.getConnection().getId(),
                value.getConnection().getName(),
                value.getProvider(),
                value.getDeliveryType(),
                value.getDestination(),
                value.getSubject(),
                value.getStatus(),
                value.getAttemptCount(),
                value.getMaximumAttempts(),
                value.getNextAttemptAt(),
                value.getProviderMessageId(),
                value.getLastErrorCategory(),
                value.getLastErrorMessage(),
                value.getCreatedByUser() == null
                        ? null : value.getCreatedByUser().getId(),
                value.getCreatedByUser() == null
                        ? null : value.getCreatedByUser().getFullName(),
                value.getCompletedAt(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    public IntegrationDeliveryAttemptResponse toResponse(
            IntegrationDeliveryAttempt value
    ) {
        return new IntegrationDeliveryAttemptResponse(
                value.getAttemptNumber(),
                value.getOutcome(),
                value.getDurationMs(),
                value.getProviderMessageId(),
                value.getErrorCategory(),
                value.getErrorMessage(),
                value.getAttemptedAt()
        );
    }
}
