package com.crm.backend.workflow;

import java.util.Map;

public record WorkflowActionResult(
        String resourceType,
        Long resourceId,
        Map<String, Object> summary
) {
}
