package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TaskRepository - Testes de Integração")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private User owner;
    private User assignee;
    private Project project;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .name("Owner").email("owner@test.com")
                .password("123").role(UserRole.ADMIN).build());

        assignee = userRepository.save(User.builder()
                .name("Lucas").email("lucas@test.com")
                .password("123").role(UserRole.MEMBER).build());

        project = projectRepository.save(Project.builder()
                .name("Projeto A").description("Desc")
                .owner(owner).build());
    }

    @Test
    @DisplayName("Deve contar tasks IN_PROGRESS por assignee")
    void shouldCountByAssigneeIdAndStatus() {
        persistTask("Task 1", "Desc 1", TaskStatus.IN_PROGRESS, assignee);
        persistTask("Task 2", "Desc 2", TaskStatus.IN_PROGRESS, assignee);
        persistTask("Task 3", "Desc 3", TaskStatus.DONE, assignee);

        long count = taskRepository.countByAssigneeIdAndStatus(assignee.getId(), TaskStatus.IN_PROGRESS);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve buscar tasks por texto no título ignorando case")
    void shouldSearchByTitleText() {
        persistTask("Corrigir Bug Login", "erro tela", TaskStatus.TODO, assignee);
        persistTask("Criar Dashboard", "painel inicial", TaskStatus.TODO, assignee);
        persistTask("Bug API", "ajustar retorno", TaskStatus.TODO, assignee);

        Page<Task> page = taskRepository.searchByText(project.getId(), "bug", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Corrigir Bug Login", "Bug API");
    }

    @Test
    @DisplayName("Deve buscar tasks por texto na descrição")
    void shouldSearchByDescriptionText() {
        persistTask("Task 1", "Problema no login", TaskStatus.TODO, assignee);
        persistTask("Task 2", "Criar tela nova", TaskStatus.TODO, assignee);

        Page<Task> page = taskRepository.searchByText(project.getId(), "login", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Task 1");
    }

    @Test
    @DisplayName("Não deve retornar tasks de outro projeto")
    void shouldNotReturnTasksFromAnotherProject() {
        Project project2 = projectRepository.save(Project.builder()
                .name("Projeto 2").description("Outro")
                .owner(owner).build());

        persistTask("Bug projeto 1", "erro", TaskStatus.TODO, assignee);

        taskRepository.save(Task.builder()
                .title("Bug projeto 2").description("erro")
                .status(TaskStatus.TODO).priority(Priority.MEDIUM)
                .project(project2).assignee(assignee).creator(owner).build());

        Page<Task> page = taskRepository.searchByText(project.getId(), "bug", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getProject().getId()).isEqualTo(project.getId());
    }

    private Task persistTask(String title, String description, TaskStatus status, User taskAssignee) {
        return taskRepository.save(Task.builder()
                .title(title).description(description)
                .status(status).priority(Priority.MEDIUM)
                .project(project).assignee(taskAssignee).creator(owner).build());
    }
}
