package com.crm.backend.task;

import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.task.dto.CreateTaskRequest;
import com.crm.backend.task.dto.TaskResponse;
import com.crm.backend.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")

public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('TASK_CREATE') and " +
                    "(#request.assignedToUserId() == null or " +
                    "#request.assignedToUserId() == authentication.principal.id or " +
                    "hasAuthority('TASK_ASSIGN'))"
    )
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, currentUser.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assignedToUserId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long leadId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(taskService.getTasks(keyword, status, priority, assignedToUserId, customerId, leadId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('TASK_UPDATE') and " +
                    "(#request.assignedToUserId() == null or " +
                    "#request.assignedToUserId() == authentication.principal.id or " +
                    "hasAuthority('TASK_ASSIGN')) and " +
                    "(#request.status() == null or " +
                    "#request.status().name() != 'COMPLETED' or " +
                    "hasAuthority('TASK_COMPLETE'))"
    )
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request, currentUser.getId()));
    }
}
