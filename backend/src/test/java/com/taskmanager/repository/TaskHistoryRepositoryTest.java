package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.entity.TaskHistory;
import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TaskHistoryRepository - Integration Tests")
class TaskHistoryRepositoryTest {

    @Autowired TaskHistoryRepository taskHistoryRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;

    private User owner;
    private Task task;

    @BeforeEach
    void setUp() {
        taskHistoryRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .name("Owner").email("owner@test.com")
                .password("123").role(UserRole.ADMIN).build());

        Project project = projectRepository.save(Project.builder()
                .name("Project").description("Desc").owner(owner).build());

        task = taskRepository.save(Task.builder()
                .title("Task").description("Desc")
                .status(TaskStatus.TODO).priority(Priority.MEDIUM)
                .project(project).creator(owner).build());
    }

    @Test
    @DisplayName("Should return history ordered by changedAt descending")
    void shouldReturnHistoryOrderedByChangedAtDesc() throws InterruptedException {
        taskHistoryRepository.save(TaskHistory.builder()
                .task(task).changedBy(owner)
                .field("status").oldValue("TODO").newValue("IN_PROGRESS")
                .build());

        Thread.sleep(50);

        taskHistoryRepository.save(TaskHistory.builder()
                .task(task).changedBy(owner)
                .field("priority").oldValue("LOW").newValue("HIGH")
                .build());

        List<TaskHistory> result = taskHistoryRepository
                .findByTaskIdOrderByChangedAtDesc(task.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getField()).isEqualTo("priority");
        assertThat(result.get(1).getField()).isEqualTo("status");
    }

    @Test
    @DisplayName("Should return empty list when task has no history")
    void shouldReturnEmptyWhenTaskHasNoHistory() {
        List<TaskHistory> result = taskHistoryRepository
                .findByTaskIdOrderByChangedAtDesc(task.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not return history from another task")
    void shouldNotReturnHistoryFromAnotherTask() {
        Task task2 = taskRepository.save(Task.builder()
                .title("Task 2").description("Desc")
                .status(TaskStatus.TODO).priority(Priority.MEDIUM)
                .project(task.getProject()).creator(owner).build());

        taskHistoryRepository.save(TaskHistory.builder()
                .task(task).changedBy(owner)
                .field("status").oldValue("TODO").newValue("DONE").build());

        taskHistoryRepository.save(TaskHistory.builder()
                .task(task2).changedBy(owner)
                .field("priority").oldValue("LOW").newValue("HIGH").build());

        List<TaskHistory> result = taskHistoryRepository
                .findByTaskIdOrderByChangedAtDesc(task.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getField()).isEqualTo("status");
    }
}
