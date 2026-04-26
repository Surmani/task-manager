package com.taskmanager.dto.response;

import com.taskmanager.domain.entity.TaskHistory;

import java.time.LocalDateTime;

public record TaskHistoryResponse(
        Long id,
        String field,
        String oldValue,
        String newValue,
        UserResponse changedBy,
        LocalDateTime changedAt
) {
    public static TaskHistoryResponse from(TaskHistory h) {
        return new TaskHistoryResponse(
                h.getId(),
                h.getField(),
                h.getOldValue(),
                h.getNewValue(),
                UserResponse.from(h.getChangedBy()),
                h.getChangedAt()
        );
    }
}
