package com.taskmanager.dto.request;

import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskUpdateRequest(
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDateTime deadline,
        Long assigneeId,
        Boolean removeAssignee
) {}
