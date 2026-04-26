package com.taskmanager.dto.response;

import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;

public record UserResponse(Long id, String name, String email, UserRole role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
