package com.crm.backend.workflow;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkflowPublicIdGenerator {

    public String workflowId() {
        return generate("wf_");
    }

    public String actionId() {
        return generate("wfa_");
    }

    public String executionId() {
        return generate("wfx_");
    }

    public String actionExecutionId() {
        return generate("wfax_");
    }

    public String claimToken() {
        return generate("wfc_");
    }

    private String generate(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
