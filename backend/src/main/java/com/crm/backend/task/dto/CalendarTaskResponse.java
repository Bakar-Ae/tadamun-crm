package com.crm.backend.task.dto;

import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskStatus;

import java.time.LocalDateTime;

public record CalendarTaskResponse(
        Long id,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueDate,
        Long assignedToUserId,
        String assignedToUserName,
        Long customerId,
        String customerName,
        Long leadId,
        String leadName
) {
}