package com.crm.backend.integration.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations.worker")
public class IntegrationDeliveryWorkerProperties {

    private boolean enabled = true;
    private int batchSize = 10;
    private long staleClaimSeconds = 120;
    private int maximumAttempts = 6;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, Math.min(batchSize, 100));
    }

    public long getStaleClaimSeconds() {
        return staleClaimSeconds;
    }

    public void setStaleClaimSeconds(long staleClaimSeconds) {
        this.staleClaimSeconds = Math.max(30, staleClaimSeconds);
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public void setMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = Math.max(1, Math.min(maximumAttempts, 10));
    }
}
