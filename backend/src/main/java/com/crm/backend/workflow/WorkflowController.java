package com.crm.backend.workflow;

import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.workflow.dto.CreateWorkflowRequest;
import com.crm.backend.workflow.dto.UpdateWorkflowRequest;
import com.crm.backend.workflow.dto.WorkflowResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ResponseEntity<Page<WorkflowResponse>> getWorkflows(
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(workflowService.getWorkflows(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                workflowService.createWorkflow(request, userDetails.getId())
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(workflowService.updateWorkflow(
                id,
                request,
                userDetails.getId()
        ));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<WorkflowResponse> activateWorkflow(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(workflowService.activateWorkflow(
                id,
                userDetails.getId()
        ));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<WorkflowResponse> pauseWorkflow(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(workflowService.pauseWorkflow(
                id,
                userDetails.getId()
        ));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<WorkflowResponse> archiveWorkflow(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(workflowService.archiveWorkflow(
                id,
                userDetails.getId()
        ));
    }
}
