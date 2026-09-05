package com.crm.backend.subscription;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class SubscriptionTimeProvider {

    public LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
