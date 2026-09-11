package com.crm.backend.integration;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntegrationPublicIdGenerator {

    public String connectionId() {
        return "int_" + compactUuid();
    }

    public String deliveryId() {
        return "idl_" + compactUuid();
    }

    public String claimToken() {
        return compactUuid();
    }

    public String idempotencyKey() {
        return "idem_" + compactUuid();
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
