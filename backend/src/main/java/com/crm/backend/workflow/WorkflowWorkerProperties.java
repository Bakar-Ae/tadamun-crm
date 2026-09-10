package com.crm.backend.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.workflows.worker")
public class WorkflowWorkerProperties {

    private int batchSize = 10;
    private long staleClaimSeconds = 120;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException(
                    "Workflow worker batch size must be between 1 and 100"
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
                    "Workflow stale claim timeout must be 30-3600 seconds"
            );
        }
        this.staleClaimSeconds = staleClaimSeconds;
    }
}
