package com.taskmanager.repository;

import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserRepository - Testes de Integração")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar usuário por email")
    void shouldFindByEmail() {
        persistUser("Lucas", "lucas@email.com", UserRole.ADMIN);

        Optional<User> result = userRepository.findByEmail("lucas@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Lucas");
        assertThat(result.get().getEmail()).isEqualTo("lucas@email.com");
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar email inexistente")
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> result = userRepository.findByEmail("naoexiste@email.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve validar existência por email")
    void shouldExistsByEmail() {
        persistUser("Lucas", "lucas@email.com", UserRole.MEMBER);

        boolean exists = userRepository.existsByEmail("lucas@email.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar falso quando email não existir")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("fake@email.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Deve buscar por parte do email ignorando case")
    void shouldSearchByEmailIgnoringCase() {
        persistUser("Lucas Surmani", "Lucas.Surmani@email.com", UserRole.ADMIN);
        persistUser("Ana Silva", "ana@email.com", UserRole.MEMBER);

        List<User> result = userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase("surmani", "xxx");

        assertThat(result)
                .hasSize(1)
                .extracting(User::getName)
                .containsExactly("Lucas Surmani");
    }

    @Test
    @DisplayName("Deve buscar por parte do nome ignorando case")
    void shouldSearchByNameIgnoringCase() {
        persistUser("Lucas Surmani", "lucas@email.com", UserRole.ADMIN);
        persistUser("Carlos Souza", "carlos@email.com", UserRole.MEMBER);

        List<User> result = userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase("zzz", "SURMANI");

        assertThat(result)
                .hasSize(1)
                .extracting(User::getName)
                .containsExactly("Lucas Surmani");
    }

    @Test
    @DisplayName("Deve buscar por email OU nome simultaneamente")
    void shouldSearchByEmailOrName() {
        persistUser("Lucas Surmani", "lucas@email.com", UserRole.ADMIN);
        persistUser("Ana Souza", "ana@email.com", UserRole.MEMBER);
        persistUser("Pedro Lucas", "pedro@email.com", UserRole.MEMBER);

        List<User> result = userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase("ana", "lucas");

        // "ana" bate no email de Ana e no nome de Pedro Lucas e Lucas Surmani
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar")
    void shouldReturnEmptyListWhenSearchFails() {
        persistUser("Lucas", "lucas@email.com", UserRole.ADMIN);

        List<User> result = userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase("zzz", "yyy");

        assertThat(result).isEmpty();
    }

    private User persistUser(String name, String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password("encoded_password")
                .role(role)
                .build());
    }
}
