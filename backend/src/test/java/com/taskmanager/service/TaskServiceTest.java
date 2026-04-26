package com.taskmanager.service;

import com.taskmanager.domain.entity.*;
import com.taskmanager.domain.enums.*;
import com.taskmanager.dto.request.*;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService - Business Rules Unit Tests")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskHistoryRepository historyRepository;
    @Mock UserRepository userRepository;
    @Mock ProjectService projectService;

    @InjectMocks TaskService taskService;

    private User admin;
    private User member;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).name("Admin")
                .email("admin@test.com").role(UserRole.ADMIN).build();
        member = User.builder().id(2L).name("Member")
                .email("member@test.com").role(UserRole.MEMBER).build();
        project = Project.builder()
                .id(1L).name("Project").owner(admin)
                .members(new ArrayList<>(List.of(member)))
                .tasks(new ArrayList<>()).build();
        task = Task.builder()
                .id(1L).title("Task")
                .status(TaskStatus.TODO).priority(Priority.MEDIUM)
                .project(project).creator(admin)
                .history(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("Should throw when moving DONE task back to TODO")
    void update_doneToTodo_throwsBadRequest() {
        task.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);

        var request = new TaskUpdateRequest(null, null, TaskStatus.TODO, null, null, null, null);

        assertThatThrownBy(() -> taskService.update(1L, request, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode voltar para A FAZER");
    }

    @Test
    @DisplayName("Should allow moving DONE task to IN_PROGRESS")
    void update_doneToInProgress_succeeds() {
        task.setStatus(TaskStatus.DONE);
        task.setAssignee(admin);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(historyRepository.saveAll(any())).thenReturn(List.of());
        when(taskRepository.save(any())).thenReturn(task);
        when(taskRepository.countByAssigneeIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(0L);

        var request = new TaskUpdateRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null, null);

        assertThatNoException().isThrownBy(() -> taskService.update(1L, request, admin));
    }

    @Test
    @DisplayName("Should throw when non-owner tries to close CRITICAL task")
    void update_criticalDoneByMember_throwsForbidden() {
        task.setPriority(Priority.CRITICAL);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(projectService.getProjectAndCheckAccess(1L, member)).thenReturn(project);

        var request = new TaskUpdateRequest(null, null, TaskStatus.DONE, null, null, null, null);

        assertThatThrownBy(() -> taskService.update(1L, request, member))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    @DisplayName("Should allow project owner to close CRITICAL task")
    void update_criticalDoneByOwner_succeeds() {
        task.setPriority(Priority.CRITICAL);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(historyRepository.saveAll(any())).thenReturn(List.of());
        when(taskRepository.save(any())).thenReturn(task);

        var request = new TaskUpdateRequest(null, null, TaskStatus.DONE, null, null, null, null);

        assertThatNoException().isThrownBy(() -> taskService.update(1L, request, admin));
    }

    @Test
    @DisplayName("Should throw when Limite de WIP is reached")
    void create_wipLimitReached_throwsBadRequest() {
        var request = new TaskRequest("New Task", null, Priority.MEDIUM, null, member.getId());
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(taskRepository.countByAssigneeIdAndStatus(member.getId(), TaskStatus.IN_PROGRESS)).thenReturn(5L);

        assertThatThrownBy(() -> taskService.create(1L, request, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Limite de WIP");
    }

    @Test
    @DisplayName("Should throw when assignee is not a project member")
    void create_assigneeNotMember_throwsBadRequest() {
        var outsider = User.builder().id(99L).name("Outsider").build();
        var request = new TaskRequest("Task", null, Priority.LOW, null, outsider.getId());
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(userRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> taskService.create(1L, request, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não é membro");
    }

    @Test
    @DisplayName("Should create task with TODO status when no assignee")
    void create_noAssignee_createsTodoTask() {
        var request = new TaskRequest("Task", "desc", Priority.HIGH, null, null);
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = taskService.create(1L, request, admin);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.title()).isEqualTo("Task");
        assertThat(response.assignee()).isNull();
    }

    @Test
    @DisplayName("Should auto-assign current user when moving to IN_PROGRESS without assignee")
    void update_moveToInProgressWithoutAssignee_autoAssigns() {
        task.setStatus(TaskStatus.TODO);
        task.setAssignee(null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(taskRepository.countByAssigneeIdAndStatus(admin.getId(), TaskStatus.IN_PROGRESS)).thenReturn(0L);
        when(historyRepository.saveAll(any())).thenReturn(List.of());
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new TaskUpdateRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null, null);
        var response = taskService.update(1L, request, admin);

        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().email()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("Should remove assignee when removeAssignee is true")
    void update_removeAssignee_clearsAssignee() {
        task.setAssignee(member);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(projectService.getProjectAndCheckAccess(1L, admin)).thenReturn(project);
        when(historyRepository.saveAll(any())).thenReturn(List.of());
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new TaskUpdateRequest(null, null, null, null, null, null, true);
        var response = taskService.update(1L, request, admin);

        assertThat(response.assignee()).isNull();
    }
}
