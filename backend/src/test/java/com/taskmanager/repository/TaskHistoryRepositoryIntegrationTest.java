package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.entity.TaskHistory;
import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskHistoryRepositoryIntegrationTest {

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deve buscar histórico por task ordenado por changedAt desc")
    void shouldFindByTaskIdOrderedByChangedAtDesc() {

        User owner = persistUser(
                "Owner",
                "owner@test.com",
                UserRole.ADMIN
        );

        User changer = persistUser(
                "Lucas",
                "lucas@test.com",
                UserRole.MEMBER
        );

        Project project = persistProject(
                "Projeto Teste",
                owner
        );

        Task task = persistTask(
                "Criar tela login",
                project,
                changer,
                owner
        );

        TaskHistory history1 = TaskHistory.builder()
                .task(task)
                .changedBy(changer)
                .field("status")
                .oldValue("TODO")
                .newValue("IN_PROGRESS")
                .build();

        entityManager.persist(history1);
        entityManager.flush();

        TaskHistory history2 = TaskHistory.builder()
                .task(task)
                .changedBy(changer)
                .field("priority")
                .oldValue("LOW")
                .newValue("HIGH")
                .build();

        entityManager.persist(history2);

        entityManager.flush();
        entityManager.clear();

        List<TaskHistory> result =
                taskHistoryRepository
                        .findByTaskIdOrderByChangedAtDesc(task.getId());

        assertThat(result).hasSize(2);

        assertThat(result.get(0).getField())
                .isEqualTo("priority");

        assertThat(result.get(1).getField())
                .isEqualTo("status");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando task não possuir histórico")
    void shouldReturnEmptyWhenTaskHasNoHistory() {

        User owner = persistUser(
                "Owner",
                "owner@test.com",
                UserRole.ADMIN
        );

        Project project = persistProject(
                "Projeto",
                owner
        );

        Task task = persistTask(
                "Task sem histórico",
                project,
                owner,
                owner
        );

        entityManager.flush();
        entityManager.clear();

        List<TaskHistory> result =
                taskHistoryRepository
                        .findByTaskIdOrderByChangedAtDesc(task.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Não deve retornar histórico de outra task")
    void shouldReturnOnlyRequestedTaskHistory() {

        User owner = persistUser(
                "Owner",
                "owner@test.com",
                UserRole.ADMIN
        );

        Project project = persistProject(
                "Projeto",
                owner
        );

        Task task1 = persistTask(
                "Task 1",
                project,
                owner,
                owner
        );

        Task task2 = persistTask(
                "Task 2",
                project,
                owner,
                owner
        );

        entityManager.persist(
                TaskHistory.builder()
                        .task(task1)
                        .changedBy(owner)
                        .field("status")
                        .oldValue("TODO")
                        .newValue("DONE")
                        .build()
        );

        entityManager.persist(
                TaskHistory.builder()
                        .task(task2)
                        .changedBy(owner)
                        .field("priority")
                        .oldValue("LOW")
                        .newValue("HIGH")
                        .build()
        );

        entityManager.flush();
        entityManager.clear();

        List<TaskHistory> result =
                taskHistoryRepository
                        .findByTaskIdOrderByChangedAtDesc(task1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getField())
                .isEqualTo("status");
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
            Project project,
            User assignee,
            User creator
    ) {
        Task task = Task.builder()
                .title(title)
                .description("Descrição")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .project(project)
                .assignee(assignee)
                .creator(creator)
                .build();

        entityManager.persist(task);
        return task;
    }
}