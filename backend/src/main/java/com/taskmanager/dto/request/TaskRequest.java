package com.taskmanager.dto.request;

import com.taskmanager.domain.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotBlank String title,
        String description,
        @NotNull Priority priority,
        LocalDateTime deadline,
        Long assigneeId
) {}
