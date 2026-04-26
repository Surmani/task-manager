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
@DisplayName("ProjectRepository - Testes de Integração")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar projetos onde usuário é owner ou member")
    void shouldFindAllByMemberOrOwner() {
        User owner = persistUser("Lucas Owner", "owner@email.com", UserRole.ADMIN);
        User member = persistUser("Lucas Member", "member@email.com", UserRole.MEMBER);
        User outsider = persistUser("Outsider", "outsider@email.com", UserRole.MEMBER);

        Project projectOwned = Project.builder()
                .name("Projeto Owner")
                .description("Usuário dono")
                .owner(owner)
                .build();

        Project projectAsMember = Project.builder()
                .name("Projeto Member")
                .description("Usuário membro")
                .owner(outsider)
                .build();
        projectAsMember.getMembers().add(owner);

        Project unrelated = Project.builder()
                .name("Projeto Sem Relação")
                .description("Não deve retornar")
                .owner(outsider)
                .build();

        projectRepository.save(projectOwned);
        projectRepository.save(projectAsMember);
        projectRepository.save(unrelated);

        List<Project> result = projectRepository.findAllByMemberOrOwner(owner.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("Projeto Owner", "Projeto Member");
    }

    @Test
    @DisplayName("Deve buscar projeto por id com members carregados")
    void shouldFindByIdWithMembers() {
        User owner = persistUser("Owner", "owner@test.com", UserRole.ADMIN);
        User member1 = persistUser("Member 1", "member1@test.com", UserRole.MEMBER);
        User member2 = persistUser("Member 2", "member2@test.com", UserRole.MEMBER);

        Project project = Project.builder()
                .name("Projeto Teste")
                .description("Descrição")
                .owner(owner)
                .build();
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
    @DisplayName("Deve retornar vazio quando id não existir")
    void shouldReturnEmptyWhenIdDoesNotExist() {
        Optional<Project> result = projectRepository.findByIdWithMembers(999L);
        assertThat(result).isEmpty();
    }

    private User persistUser(String name, String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(name).email(email)
                .password("encoded_password").role(role)
                .build());
    }
}
