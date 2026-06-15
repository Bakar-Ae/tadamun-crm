package com.crm.backend.task;

import com.crm.backend.task.dto.TaskResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(CrmTask task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getAssignedToUser() == null ? null : task.getAssignedToUser().getId(),
                task.getAssignedToUser() == null ? null : task.getAssignedToUser().getFullName(),
                task.getCustomer() == null ? null : task.getCustomer().getId(),
                task.getCustomer() == null ? null : task.getCustomer().getName(),
                task.getLead() == null ? null : task.getLead().getId(),
                task.getLead() == null ? null : task.getLead().getFullName(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}