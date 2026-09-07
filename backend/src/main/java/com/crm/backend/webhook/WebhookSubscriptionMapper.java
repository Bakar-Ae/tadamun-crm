package com.crm.backend.webhook;

import com.crm.backend.user.User;
import com.crm.backend.webhook.dto.WebhookSubscriptionResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Component
public class WebhookSubscriptionMapper {

    public WebhookSubscriptionResponse toResponse(
            WebhookSubscription subscription,
            Collection<WebhookSubscriptionEvent> events
    ) {
        User createdBy = subscription.getCreatedByUser();
        User updatedBy = subscription.getUpdatedByUser();
        User revokedBy = subscription.getRevokedByUser();

        return new WebhookSubscriptionResponse(
                subscription.getId(),
                subscription.getName(),
                subscription.getEndpointUrl(),
                subscription.getStatus(),
                subscription.getSecretDisplaySuffix(),
                events.stream()
                        .map(event -> event.getId().getEventType().getValue())
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                subscription.getConsecutiveFailures(),
                subscription.getLastSuccessAt(),
                subscription.getLastFailureAt(),
                createdBy.getId(),
                createdBy.getFullName(),
                updatedBy == null ? null : updatedBy.getId(),
                updatedBy == null ? null : updatedBy.getFullName(),
                revokedBy == null ? null : revokedBy.getId(),
                revokedBy == null ? null : revokedBy.getFullName(),
                subscription.getRevokedAt(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
