package com.crm.backend.task.dto;

import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueDate,
        Long assignedToUserId,
        String assignedToUserName,
        Long customerId,
        String customerName,
        Long leadId,
        String leadName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}