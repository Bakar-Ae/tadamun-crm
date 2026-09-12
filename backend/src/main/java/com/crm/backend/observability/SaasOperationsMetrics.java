package com.crm.backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SaasOperationsMetrics {

    private static final Logger log = LoggerFactory.getLogger(
            SaasOperationsMetrics.class
    );

    private final MeterRegistry meterRegistry;

    public SaasOperationsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(Subsystem subsystem, Outcome outcome) {
        record(subsystem, outcome, 1);
    }

    public void record(
            Subsystem subsystem,
            Outcome outcome,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }
        try {
            Counter.builder("crm.operations")
                    .description("Tadamun CRM background operation outcomes")
                    .tag("subsystem", subsystem.tagValue)
                    .tag("outcome", outcome.tagValue)
                    .register(meterRegistry)
                    .increment(amount);
        } catch (RuntimeException metricFailure) {
            log.warn(
                    "Operation metric recording failed. subsystem={}, reason={}",
                    subsystem.tagValue,
                    metricFailure.getClass().getSimpleName()
            );
        }
    }

    public enum Subsystem {
        BILLING_WEBHOOK("billing_webhook"),
        INTEGRATION_DELIVERY("integration_delivery"),
        WEBHOOK_DELIVERY("webhook_delivery"),
        WEBHOOK_EVENT("webhook_event"),
        WORKFLOW_ACTION("workflow_action");

        private final String tagValue;

        Subsystem(String tagValue) {
            this.tagValue = tagValue;
        }
    }

    public enum Outcome {
        COMPLETED("completed"),
        DUPLICATE("duplicate"),
        FAILED("failed"),
        IGNORED("ignored"),
        RECOVERED("recovered");

        private final String tagValue;

        Outcome(String tagValue) {
            this.tagValue = tagValue;
        }
    }
}
