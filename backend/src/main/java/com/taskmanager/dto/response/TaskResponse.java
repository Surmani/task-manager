package com.taskmanager.dto.response;

import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDateTime deadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long projectId,
        UserResponse assignee,
        UserResponse creator
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getDeadline(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getProject().getId(),
                t.getAssignee() != null ? UserResponse.from(t.getAssignee()) : null,
                UserResponse.from(t.getCreator())
        );
    }
}
