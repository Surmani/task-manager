package com.taskmanager.dto.response;

import com.taskmanager.domain.entity.Project;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        UserResponse owner,
        List<UserResponse> members,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                UserResponse.from(p.getOwner()),
                p.getMembers().stream().map(UserResponse::from).toList(),
                p.getCreatedAt()
        );
    }
}
