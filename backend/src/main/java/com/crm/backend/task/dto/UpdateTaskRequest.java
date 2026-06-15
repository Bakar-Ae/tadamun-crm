package com.crm.backend.task.dto;

import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200)
        String title,

        String description,

        @NotNull(message = "Status is required")
        TaskStatus status,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        LocalDateTime dueDate,

        Long assignedToUserId,

        Long customerId,

        Long leadId
) {
}