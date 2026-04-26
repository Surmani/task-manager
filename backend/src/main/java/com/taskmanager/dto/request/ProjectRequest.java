package com.taskmanager.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ProjectRequest(
        @NotBlank String name,
        String description,
        List<Long> memberIds
) {}
