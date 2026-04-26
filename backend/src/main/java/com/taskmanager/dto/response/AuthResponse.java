package com.taskmanager.dto.response;

public record AuthResponse(String token, UserResponse user) {}
