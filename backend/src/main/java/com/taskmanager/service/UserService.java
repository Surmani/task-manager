package com.taskmanager.service;

import com.taskmanager.dto.response.UserResponse;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> search(String q) {
        return userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(q, q)
                .stream().map(UserResponse::from).toList();
    }
}
