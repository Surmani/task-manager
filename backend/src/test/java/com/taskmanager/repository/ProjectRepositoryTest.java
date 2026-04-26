package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
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
@DisplayName("ProjectRepository - Integration Tests")
class ProjectRepositoryTest {

    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find projects where user is owner or member")
    void shouldFindAllByMemberOrOwner() {
        User owner = persistUser("Owner", "owner@email.com", UserRole.ADMIN);
        User outsider = persistUser("Outsider", "outsider@email.com", UserRole.MEMBER);

        Project projectOwned = projectRepository.save(Project.builder()
                .name("Owned Project").description("desc").owner(owner).build());

        Project projectAsMember = Project.builder()
                .name("Member Project").description("desc").owner(outsider).build();
        projectAsMember.getMembers().add(owner);
        projectRepository.save(projectAsMember);

        projectRepository.save(Project.builder()
                .name("Unrelated Project").description("desc").owner(outsider).build());

        List<Project> result = projectRepository.findAllByMemberOrOwner(owner.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("Owned Project", "Member Project");
    }

    @Test
    @DisplayName("Should find project by id with members loaded")
    void shouldFindByIdWithMembers() {
        User owner = persistUser("Owner", "owner@test.com", UserRole.ADMIN);
        User member1 = persistUser("Member 1", "member1@test.com", UserRole.MEMBER);
        User member2 = persistUser("Member 2", "member2@test.com", UserRole.MEMBER);

        Project project = Project.builder()
                .name("Project").description("desc").owner(owner).build();
        project.getMembers().add(member1);
        project.getMembers().add(member2);
        projectRepository.save(project);

        Optional<Project> result = projectRepository.findByIdWithMembers(project.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getMembers())
                .hasSize(2)
                .extracting(User::getName)
                .containsExactlyInAnyOrder("Member 1", "Member 2");
    }

    @Test
    @DisplayName("Should return empty when project id does not exist")
    void shouldReturnEmptyWhenIdDoesNotExist() {
        Optional<Project> result = projectRepository.findByIdWithMembers(999L);
        assertThat(result).isEmpty();
    }

    private User persistUser(String name, String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(name).email(email)
                .password("encoded_password").role(role).build());
    }
}
