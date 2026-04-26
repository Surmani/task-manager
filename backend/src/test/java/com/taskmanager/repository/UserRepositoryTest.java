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
@DisplayName("UserRepository - Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        persistUser("Lucas", "lucas@email.com", UserRole.ADMIN);

        Optional<User> result = userRepository.findByEmail("lucas@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Lucas");
        assertThat(result.get().getEmail()).isEqualTo("lucas@email.com");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> result = userRepository.findByEmail("notfound@email.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldExistsByEmail() {
        persistUser("Lucas", "lucas@email.com", UserRole.MEMBER);

        boolean exists = userRepository.existsByEmail("lucas@email.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("fake@email.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should search by email ignoring case")
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
    @DisplayName("Should search by name ignoring case")
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
    @DisplayName("Should search by email OR name simultaneously")
    void shouldSearchByEmailOrName() {
        persistUser("Lucas Surmani", "lucas@email.com", UserRole.ADMIN);
        persistUser("Ana Souza", "ana@email.com", UserRole.MEMBER);
        persistUser("Pedro Lucas", "pedro@email.com", UserRole.MEMBER);

        List<User> result = userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase("ana", "lucas");

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Should return empty list when search finds nothing")
    void shouldReturnEmptyListWhenSearchFails() {
        persistUser("Lucas", "lucas@email.com", UserRole.ADMIN);

        List<User> result = userRepository
                .findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase("zzz", "yyy");

        assertThat(result).isEmpty();
    }

    private User persistUser(String name, String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(name).email(email)
                .password("encoded_password").role(role)
                .build());
    }
}
