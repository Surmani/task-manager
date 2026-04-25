package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deve contar tasks por assignee e status")
    void shouldCountByAssigneeIdAndStatus() {

        User owner = persistUser("Owner", "owner@test.com", UserRole.ADMIN);
        User assignee = persistUser("Lucas", "lucas@test.com", UserRole.MEMBER);

        Project project = persistProject("Projeto A", owner);

        persistTask("Task 1", "Desc 1", TaskStatus.TODO, project, assignee, owner);
        persistTask("Task 2", "Desc 2", TaskStatus.TODO, project, assignee, owner);
        persistTask("Task 3", "Desc 3", TaskStatus.DONE, project, assignee, owner);

        entityManager.flush();
        entityManager.clear();

        long count =
                taskRepository.countByAssigneeIdAndStatus(
                        assignee.getId(),
                        TaskStatus.TODO
                );

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve buscar tasks por texto no título")
    void shouldSearchByTitleText() {

        User owner = persistUser("Owner", "owner@test.com", UserRole.ADMIN);
        User assignee = persistUser("Lucas", "lucas@test.com", UserRole.MEMBER);

        Project project = persistProject("Projeto Busca", owner);

        persistTask("Corrigir Bug Login", "erro tela", TaskStatus.TODO, project, assignee, owner);
        persistTask("Criar Dashboard", "painel inicial", TaskStatus.TODO, project, assignee, owner);
        persistTask("Bug API", "ajustar retorno", TaskStatus.TODO, project, assignee, owner);

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

        Page<Task> page =
                taskRepository.searchByText(
                        project.getId(),
                        "bug",
                        pageable
                );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder(
                        "Corrigir Bug Login",
                        "Bug API"
                );
    }

    @Test
    @DisplayName("Deve buscar tasks por texto na descrição")
    void shouldSearchByDescriptionText() {

        User owner = persistUser("Owner", "owner@test.com", UserRole.ADMIN);
        User assignee = persistUser("Lucas", "lucas@test.com", UserRole.MEMBER);

        Project project = persistProject("Projeto Busca Desc", owner);

        persistTask("Task 1", "Problema no login", TaskStatus.TODO, project, assignee, owner);
        persistTask("Task 2", "Criar tela nova", TaskStatus.TODO, project, assignee, owner);

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> page =
                taskRepository.searchByText(
                        project.getId(),
                        "login",
                        pageable
                );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle())
                .isEqualTo("Task 1");
    }

    @Test
    @DisplayName("Não deve buscar tasks de outro projeto")
    void shouldNotReturnTasksFromAnotherProject() {

        User owner = persistUser("Owner", "owner@test.com", UserRole.ADMIN);
        User assignee = persistUser("Lucas", "lucas@test.com", UserRole.MEMBER);

        Project project1 = persistProject("Projeto 1", owner);
        Project project2 = persistProject("Projeto 2", owner);

        persistTask("Bug projeto 1", "erro", TaskStatus.TODO, project1, assignee, owner);
        persistTask("Bug projeto 2", "erro", TaskStatus.TODO, project2, assignee, owner);

        entityManager.flush();
        entityManager.clear();

        Page<Task> page =
                taskRepository.searchByText(
                        project1.getId(),
                        "bug",
                        PageRequest.of(0, 10)
                );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getProject().getId())
                .isEqualTo(project1.getId());
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

    private Project persistProject(
            String name,
            User owner
    ) {
        Project project = Project.builder()
                .name(name)
                .description("Projeto teste")
                .owner(owner)
                .build();

        entityManager.persist(project);
        return project;
    }

    private Task persistTask(
            String title,
            String description,
            TaskStatus status,
            Project project,
            User assignee,
            User creator
    ) {
        Task task = Task.builder()
                .title(title)
                .description(description)
                .status(status)
                .priority(Priority.MEDIUM)
                .project(project)
                .assignee(assignee)
                .creator(creator)
                .build();

        entityManager.persist(task);
        return task;
    }
}