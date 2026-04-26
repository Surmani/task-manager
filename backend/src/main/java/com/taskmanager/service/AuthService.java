package com.taskmanager.service;

import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;
import com.taskmanager.dto.request.LoginRequest;
import com.taskmanager.dto.request.RegisterRequest;
import com.taskmanager.dto.response.AuthResponse;
import com.taskmanager.dto.response.UserResponse;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        var user = userRepository.findByEmail(request.email()).orElseThrow();
        var token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw BusinessException.badRequest("Email already registered");
        }
        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role() != null ? request.role() : UserRole.MEMBER)
                .build();
        userRepository.save(user);
        var token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
