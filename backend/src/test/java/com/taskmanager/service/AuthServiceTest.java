package com.taskmanager.service;

import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;
import com.taskmanager.dto.request.LoginRequest;
import com.taskmanager.dto.request.RegisterRequest;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .name("Lucas")
                .email("lucas@test.com")
                .password("encoded_password")
                .role(UserRole.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Should return token and user on successful login")
    void login_validCredentials_returnsAuthResponse() {
        var request = new LoginRequest("lucas@test.com", "123456");
        when(userRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken("lucas@test.com")).thenReturn("mock.jwt.token");

        var response = authService.login(request);

        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.user().email()).isEqualTo("lucas@test.com");
        assertThat(response.user().role()).isEqualTo(UserRole.ADMIN);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken("lucas@test.com");
    }

    @Test
    @DisplayName("Should throw BusinessException when email already exists")
    void register_duplicateEmail_throwsBusinessException() {
        var request = new RegisterRequest("Lucas", "lucas@test.com", "123456", UserRole.MEMBER);
        when(userRepository.existsByEmail("lucas@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create user and return token on successful register")
    void register_newEmail_createsUserAndReturnsToken() {
        var request = new RegisterRequest("Lucas", "lucas@test.com", "123456", UserRole.ADMIN);
        when(userRepository.existsByEmail("lucas@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken("lucas@test.com")).thenReturn("mock.jwt.token");

        var response = authService.register(request);

        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.user().name()).isEqualTo("Lucas");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should default to MEMBER role when role is null")
    void register_nullRole_defaultsMemberRole() {
        var request = new RegisterRequest("Lucas", "lucas@test.com", "123456", null);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getRole()).isEqualTo(UserRole.MEMBER);
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register(request);

        verify(userRepository).save(any(User.class));
    }
}
