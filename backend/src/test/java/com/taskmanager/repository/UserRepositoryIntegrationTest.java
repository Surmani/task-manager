package com.taskmanager.repository;

import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deve buscar usuário por email")
    void shouldFindByEmail() {

        persistUser(
                "Lucas",
                "lucas@email.com",
                UserRole.ADMIN
        );

        entityManager.flush();
        entityManager.clear();

        Optional<User> result =
                userRepository.findByEmail("lucas@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Lucas");
        assertThat(result.get().getEmail())
                .isEqualTo("lucas@email.com");
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar email inexistente")
    void shouldReturnEmptyWhenEmailNotFound() {

        Optional<User> result =
                userRepository.findByEmail("naoexiste@email.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve validar existência por email")
    void shouldExistsByEmail() {

        persistUser(
                "Lucas",
                "lucas@email.com",
                UserRole.MEMBER
        );

        entityManager.flush();
        entityManager.clear();

        boolean exists =
                userRepository.existsByEmail("lucas@email.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar falso quando email não existir")
    void shouldReturnFalseWhenEmailDoesNotExist() {

        boolean exists =
                userRepository.existsByEmail("fake@email.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Deve buscar por parte do email ignorando case")
    void shouldSearchByEmailIgnoringCase() {

        persistUser(
                "Lucas Marçal",
                "Lucas.Surmani@email.com",
                UserRole.ADMIN
        );

        persistUser(
                "Ana Silva",
                "ana@email.com",
                UserRole.MEMBER
        );

        entityManager.flush();
        entityManager.clear();

        List<User> result =
                userRepository
                        .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
                                "surmani",
                                "xxx"
                        );

        assertThat(result)
                .hasSize(1)
                .extracting(User::getName)
                .containsExactly("Lucas Marçal");
    }

    @Test
    @DisplayName("Deve buscar por parte do nome ignorando case")
    void shouldSearchByNameIgnoringCase() {

        persistUser(
                "Lucas Marçal",
                "lucas@email.com",
                UserRole.ADMIN
        );

        persistUser(
                "Carlos Souza",
                "carlos@email.com",
                UserRole.MEMBER
        );

        entityManager.flush();
        entityManager.clear();

        List<User> result =
                userRepository
                        .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
                                "zzz",
                                "MARÇAL"
                        );

        assertThat(result)
                .hasSize(1)
                .extracting(User::getName)
                .containsExactly("Lucas Marçal");
    }

    @Test
    @DisplayName("Deve buscar por email OU nome")
    void shouldSearchByEmailOrName() {

        persistUser(
                "Lucas Marçal",
                "lucas@email.com",
                UserRole.ADMIN
        );

        persistUser(
                "Ana Souza",
                "ana@email.com",
                UserRole.MEMBER
        );

        persistUser(
                "Pedro Lucas",
                "pedro@email.com",
                UserRole.MEMBER
        );

        entityManager.flush();
        entityManager.clear();

        List<User> result =
                userRepository
                        .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
                                "ana",
                                "lucas"
                        );

        assertThat(result)
                .hasSize(3);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar")
    void shouldReturnEmptyListWhenSearchFails() {

        persistUser(
                "Lucas",
                "lucas@email.com",
                UserRole.ADMIN
        );

        entityManager.flush();
        entityManager.clear();

        List<User> result =
                userRepository
                        .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
                                "zzz",
                                "yyy"
                        );

        assertThat(result).isEmpty();
    }

    private User persistUser(
            String name,
            String email,
            UserRole role
    ) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password("123456")
                .role(role)
                .build();

        entityManager.persist(user);
        return user;
    }
}