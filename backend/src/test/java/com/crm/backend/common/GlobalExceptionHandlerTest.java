package com.crm.backend.common;

import com.crm.backend.subscription.SubscriptionFeature;
import com.crm.backend.subscription.usage.SubscriptionLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void subscriptionLimitShouldReturnUpgradeRequiredResponse() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        SubscriptionLimitExceededException exception =
                new SubscriptionLimitExceededException(
                        SubscriptionFeature.MEMBERS,
                        5L,
                        5L
                );

        ResponseEntity<Map<String, Object>> response =
                handler.handleSubscriptionLimitExceeded(exception);

        assertEquals(402, response.getStatusCode().value());
        assertEquals(
                "SUBSCRIPTION_LIMIT_REACHED",
                response.getBody().get("code")
        );
        assertEquals("MEMBERS", response.getBody().get("feature"));
        assertEquals(true, response.getBody().get("upgradeRequired"));
    }
}
