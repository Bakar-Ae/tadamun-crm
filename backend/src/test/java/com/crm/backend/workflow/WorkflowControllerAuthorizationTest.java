package com.crm.backend.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowControllerAuthorizationTest {

    @Test
    void readEndpointsShouldRequireWorkflowViewPermission()
            throws Exception {
        assertAuthorization(
                "getWorkflows",
                "WORKFLOW_VIEW",
                org.springframework.data.domain.Pageable.class
        );
        assertAuthorization("getWorkflow", "WORKFLOW_VIEW", Long.class);
    }

    @Test
    void writeEndpointsShouldRequireWorkflowManagePermission()
            throws Exception {
        assertAuthorization(
                "createWorkflow",
                "WORKFLOW_MANAGE",
                com.crm.backend.workflow.dto.CreateWorkflowRequest.class,
                com.crm.backend.security.CustomUserDetails.class
        );
        assertAuthorization(
                "updateWorkflow",
                "WORKFLOW_MANAGE",
                Long.class,
                com.crm.backend.workflow.dto.UpdateWorkflowRequest.class,
                com.crm.backend.security.CustomUserDetails.class
        );
        assertAuthorization(
                "activateWorkflow",
                "WORKFLOW_MANAGE",
                Long.class,
                com.crm.backend.security.CustomUserDetails.class
        );
        assertAuthorization(
                "pauseWorkflow",
                "WORKFLOW_MANAGE",
                Long.class,
                com.crm.backend.security.CustomUserDetails.class
        );
        assertAuthorization(
                "archiveWorkflow",
                "WORKFLOW_MANAGE",
                Long.class,
                com.crm.backend.security.CustomUserDetails.class
        );
    }

    private void assertAuthorization(
            String methodName,
            String authority,
            Class<?>... parameterTypes
    ) throws Exception {
        Method method = WorkflowController.class.getMethod(
                methodName,
                parameterTypes
        );
        assertEquals(
                "hasAuthority('" + authority + "')",
                method.getAnnotation(PreAuthorize.class).value()
        );
    }
}
