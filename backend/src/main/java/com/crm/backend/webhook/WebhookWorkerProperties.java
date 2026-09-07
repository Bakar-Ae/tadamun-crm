package com.crm.backend.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhooks.worker")
public class WebhookWorkerProperties {

    private int batchSize = 10;
    private long staleClaimSeconds = 120;
    private int eventMaximumAttempts = 5;
    private int failedSubscriptionThreshold = 10;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Webhook worker batch size must be between 1 and 100"
            );
        }
        this.batchSize = batchSize;
    }

    public long getStaleClaimSeconds() {
        return staleClaimSeconds;
    }

    public void setStaleClaimSeconds(long staleClaimSeconds) {
        if (staleClaimSeconds < 30 || staleClaimSeconds > 3600) {
            throw new IllegalArgumentException(
                    "Webhook stale claim timeout must be 30-3600 seconds"
            );
        }
        this.staleClaimSeconds = staleClaimSeconds;
    }

    public int getEventMaximumAttempts() {
        return eventMaximumAttempts;
    }

    public void setEventMaximumAttempts(int eventMaximumAttempts) {
        if (eventMaximumAttempts < 1 || eventMaximumAttempts > 20) {
            throw new IllegalArgumentException(
                    "Webhook event maximum attempts must be between 1 and 20"
            );
        }
        this.eventMaximumAttempts = eventMaximumAttempts;
    }

    public int getFailedSubscriptionThreshold() {
        return failedSubscriptionThreshold;
    }

    public void setFailedSubscriptionThreshold(
            int failedSubscriptionThreshold
    ) {
        if (failedSubscriptionThreshold < 1
                || failedSubscriptionThreshold > 100) {
            throw new IllegalArgumentException(
                    "Webhook failure threshold must be between 1 and 100"
            );
        }
        this.failedSubscriptionThreshold = failedSubscriptionThreshold;
    }
}
