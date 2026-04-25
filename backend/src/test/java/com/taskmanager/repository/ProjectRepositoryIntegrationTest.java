package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
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
class ProjectRepositoryIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deve buscar projetos onde usuário é owner ou member")
    void shouldFindAllByMemberOrOwner() {
        User owner = createUser(
                "Lucas Owner",
                "owner@email.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "Lucas Member",
                "member@email.com",
                UserRole.MEMBER
        );

        User outsider = createUser(
                "Outsider",
                "outsider@email.com",
                UserRole.MEMBER
        );

        entityManager.persist(owner);
        entityManager.persist(member);
        entityManager.persist(outsider);

        Project projectOwner = Project.builder()
                .name("Projeto Owner")
                .description("Usuário dono")
                .owner(owner)
                .build();

        Project projectMember = Project.builder()
                .name("Projeto Member")
                .description("Usuário membro")
                .owner(outsider)
                .build();

        projectMember.getMembers().add(owner);

        Project unrelated = Project.builder()
                .name("Projeto Sem Relação")
                .description("Não deve retornar")
                .owner(outsider)
                .build();

        entityManager.persist(projectOwner);
        entityManager.persist(projectMember);
        entityManager.persist(unrelated);

        entityManager.flush();
        entityManager.clear();

        List<Project> result =
                projectRepository.findAllByMemberOrOwner(owner.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(Project::getName)
                .containsExactlyInAnyOrder(
                        "Projeto Owner",
                        "Projeto Member"
                );
    }

    @Test
    @DisplayName("Deve buscar projeto por id com members carregados")
    void shouldFindByIdWithMembers() {
        User owner = createUser(
                "Owner",
                "owner@test.com",
                UserRole.ADMIN
        );

        User member1 = createUser(
                "Member 1",
                "member1@test.com",
                UserRole.MEMBER
        );

        User member2 = createUser(
                "Member 2",
                "member2@test.com",
                UserRole.MEMBER
        );

        entityManager.persist(owner);
        entityManager.persist(member1);
        entityManager.persist(member2);

        Project project = Project.builder()
                .name("Projeto Teste")
                .description("Descrição")
                .owner(owner)
                .build();

        project.getMembers().add(member1);
        project.getMembers().add(member2);

        entityManager.persist(project);

        entityManager.flush();
        entityManager.clear();

        Optional<Project> result =
                projectRepository.findByIdWithMembers(project.getId());

        assertThat(result).isPresent();

        assertThat(result.get().getMembers())
                .hasSize(2)
                .extracting(User::getName)
                .containsExactlyInAnyOrder(
                        "Member 1",
                        "Member 2"
                );
    }

    @Test
    @DisplayName("Deve retornar vazio quando id não existir")
    void shouldReturnEmptyWhenIdDoesNotExist() {
        Optional<Project> result =
                projectRepository.findByIdWithMembers(999L);
        assertThat(result).isEmpty();
    }

    private User createUser(
            String name,
            String email,
            UserRole role
    ) {
        return User.builder()
                .name(name)
                .email(email)
                .password("123456")
                .role(role)
                .build();
    }
}