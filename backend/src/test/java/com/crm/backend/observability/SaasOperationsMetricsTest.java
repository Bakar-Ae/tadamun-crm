package com.crm.backend.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaasOperationsMetricsTest {

    @Test
    void operationCountersShouldUseBoundedTagsAndSupportBatchRecovery() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SaasOperationsMetrics metrics = new SaasOperationsMetrics(registry);

        metrics.record(
                SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                SaasOperationsMetrics.Outcome.RECOVERED,
                3
        );
        metrics.record(
                SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                SaasOperationsMetrics.Outcome.FAILED
        );
        metrics.record(
                SaasOperationsMetrics.Subsystem.WEBHOOK_DELIVERY,
                SaasOperationsMetrics.Outcome.FAILED,
                0
        );

        assertEquals(
                3.0,
                registry.get("crm.operations")
                        .tags(
                                "subsystem", "webhook_delivery",
                                "outcome", "recovered"
                        )
                        .counter()
                        .count()
        );
        assertEquals(
                1.0,
                registry.get("crm.operations")
                        .tags(
                                "subsystem", "webhook_delivery",
                                "outcome", "failed"
                        )
                        .counter()
                        .count()
        );
    }
}
