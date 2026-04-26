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
@DisplayName("TaskRepository - Integration Tests")
class TaskRepositoryTest {

    @Autowired TaskRepository taskRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;

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
                .name("Project A").description("Desc").owner(owner).build());
    }

    @Test
    @DisplayName("Should count IN_PROGRESS tasks by assignee")
    void shouldCountByAssigneeIdAndStatus() {
        persistTask("Task 1", "Desc", TaskStatus.IN_PROGRESS, assignee);
        persistTask("Task 2", "Desc", TaskStatus.IN_PROGRESS, assignee);
        persistTask("Task 3", "Desc", TaskStatus.DONE, assignee);

        long count = taskRepository.countByAssigneeIdAndStatus(assignee.getId(), TaskStatus.IN_PROGRESS);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search tasks by title text ignoring case")
    void shouldSearchByTitleText() {
        persistTask("Fix Login Bug", "error screen", TaskStatus.TODO, assignee);
        persistTask("Create Dashboard", "main panel", TaskStatus.TODO, assignee);
        persistTask("Bug in API", "fix return", TaskStatus.TODO, assignee);

        Page<Task> page = taskRepository.searchByText(project.getId(), "bug", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Fix Login Bug", "Bug in API");
    }

    @Test
    @DisplayName("Should search tasks by description text")
    void shouldSearchByDescriptionText() {
        persistTask("Task 1", "Problem in login", TaskStatus.TODO, assignee);
        persistTask("Task 2", "Create new screen", TaskStatus.TODO, assignee);

        Page<Task> page = taskRepository.searchByText(project.getId(), "login", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Task 1");
    }

    @Test
    @DisplayName("Should not return tasks from another project")
    void shouldNotReturnTasksFromAnotherProject() {
        Project project2 = projectRepository.save(Project.builder()
                .name("Project 2").description("Other").owner(owner).build());

        persistTask("Bug in project 1", "error", TaskStatus.TODO, assignee);
        taskRepository.save(Task.builder()
                .title("Bug in project 2").description("error")
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
