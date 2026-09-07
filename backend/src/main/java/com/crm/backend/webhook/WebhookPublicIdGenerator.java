package com.crm.backend.webhook;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebhookPublicIdGenerator {

    public String eventId() {
        return generate("evt_");
    }

    public String deliveryId() {
        return generate("dlv_");
    }

    public String claimToken() {
        return generate("clm_");
    }

    private String generate(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
