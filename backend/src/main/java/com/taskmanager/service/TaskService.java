package com.taskmanager.service;

import com.taskmanager.domain.entity.*;
import com.taskmanager.domain.enums.*;
import com.taskmanager.dto.request.*;
import com.taskmanager.dto.response.*;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int WIP_LIMIT = 5;

    private final TaskRepository taskRepository;
    private final TaskHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    public PageResponse<TaskResponse> findAll(Long projectId, User currentUser,
                                              TaskStatus status, Priority priority,
                                              Long assigneeId, LocalDateTime from,
                                              LocalDateTime to, String sortBy,
                                              int page, int size) {
        projectService.getProjectAndCheckAccess(projectId, currentUser);

        Specification<Task> spec = Specification.where(
                (root, q, cb) -> cb.equal(root.get("project").get("id"), projectId)
        );
        if (status != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (priority != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("priority"), priority));
        if (assigneeId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("assignee").get("id"), assigneeId));
        if (from != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), from));
        if (to != null)
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("createdAt"), to));

        Sort sort = switch (sortBy != null ? sortBy : "createdAt") {
            case "priority" -> Sort.by("priority").descending();
            case "deadline" -> Sort.by("deadline").ascending();
            default -> Sort.by("createdAt").descending();
        };

        var pageable = PageRequest.of(page, size, sort);
        return PageResponse.from(taskRepository.findAll(spec, pageable).map(TaskResponse::from));
    }

    public PageResponse<TaskResponse> search(Long projectId, String text, User currentUser, int page, int size) {
        projectService.getProjectAndCheckAccess(projectId, currentUser);
        var pageable = PageRequest.of(page, size);
        return PageResponse.from(taskRepository.searchByText(projectId, text, pageable).map(TaskResponse::from));
    }

    public ProjectReportResponse getReport(Long projectId, User currentUser) {
        projectService.getProjectAndCheckAccess(projectId, currentUser);
        var tasks = taskRepository.findAll(
                (root, q, cb) -> cb.equal(root.get("project").get("id"), projectId)
        );

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) byStatus.put(s.name(), 0L);

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) byPriority.put(p.name(), 0L);

        for (Task t : tasks) {
            byStatus.merge(t.getStatus().name(), 1L, Long::sum);
            byPriority.merge(t.getPriority().name(), 1L, Long::sum);
        }

        return new ProjectReportResponse(byStatus, byPriority);
    }

    public List<TaskHistoryResponse> getHistory(Long taskId, User currentUser) {
        var task = getTask(taskId);
        projectService.getProjectAndCheckAccess(task.getProject().getId(), currentUser);
        return historyRepository.findByTaskIdOrderByChangedAtDesc(taskId)
                .stream().map(TaskHistoryResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(Long projectId, TaskRequest request, User currentUser) {
        var project = projectService.getProjectAndCheckAccess(projectId, currentUser);

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = resolveAssignee(request.assigneeId(), project);
            checkWipLimit(assignee, null);
        }

        var task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(TaskStatus.TODO)
                .priority(request.priority())
                .deadline(request.deadline())
                .project(project)
                .assignee(assignee)
                .creator(currentUser)
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskUpdateRequest request, User currentUser) {
        var task = getTask(taskId);
        var project = projectService.getProjectAndCheckAccess(task.getProject().getId(), currentUser);

        List<TaskHistory> historyEntries = new ArrayList<>();

        if (request.status() != null && !request.status().equals(task.getStatus())) {
            validateStatusTransition(task.getStatus(), request.status(), currentUser, task.getPriority(), project);

            // Auto-assignment: moving to IN_PROGRESS without assignee assigns the current user
            if (request.status() == TaskStatus.IN_PROGRESS && task.getAssignee() == null) {
                checkWipLimit(currentUser, taskId);
                historyEntries.add(buildHistory(task, currentUser, "assignee", null, currentUser.getName()));
                task.setAssignee(currentUser);
            }

            if (request.status() == TaskStatus.IN_PROGRESS && task.getAssignee() != null) {
                checkWipLimit(task.getAssignee(), taskId);
            }

            historyEntries.add(buildHistory(task, currentUser, "status",
                    task.getStatus().name(), request.status().name()));
            task.setStatus(request.status());
        }

        if (request.title() != null && !request.title().equals(task.getTitle())) {
            historyEntries.add(buildHistory(task, currentUser, "title", task.getTitle(), request.title()));
            task.setTitle(request.title());
        }

        if (request.description() != null) task.setDescription(request.description());
        if (request.deadline() != null) task.setDeadline(request.deadline());

        if (request.priority() != null && !request.priority().equals(task.getPriority())) {
            historyEntries.add(buildHistory(task, currentUser, "priority",
                    task.getPriority().name(), request.priority().name()));
            task.setPriority(request.priority());
        }

        if (Boolean.TRUE.equals(request.removeAssignee())) {
            if (task.getAssignee() != null) {
                historyEntries.add(buildHistory(task, currentUser, "assignee",
                        task.getAssignee().getName(), null));
            }
            task.setAssignee(null);
        } else if (request.assigneeId() != null) {
            var newAssignee = resolveAssignee(request.assigneeId(), project);
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(newAssignee.getId())) {
                if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                    checkWipLimit(newAssignee, taskId);
                }
                historyEntries.add(buildHistory(task, currentUser, "assignee",
                        task.getAssignee() != null ? task.getAssignee().getName() : null,
                        newAssignee.getName()));
                task.setAssignee(newAssignee);
            }
        }

        historyRepository.saveAll(historyEntries);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long taskId, User currentUser) {
        var task = getTask(taskId);
        projectService.getProjectAndCheckAccess(task.getProject().getId(), currentUser);
        taskRepository.delete(task);
    }

    private void validateStatusTransition(TaskStatus current, TaskStatus next,
                                          User user, Priority priority, Project project) {
        if (current == TaskStatus.DONE && next == TaskStatus.TODO) {
            throw BusinessException.badRequest("Uma tarefa CONCLUÍDA não pode voltar para A FAZER");
        }
        if (next == TaskStatus.DONE && priority == Priority.CRITICAL) {
            boolean isOwner = project.getOwner().getId().equals(user.getId());
            if (!isOwner) {
                throw BusinessException.forbidden("Apenas o ADMIN do projeto pode concluir tarefas CRÍTICAS");
            }
        }
    }

    private void checkWipLimit(User assignee, Long excludeTaskId) {
        long count = taskRepository.countByAssigneeIdAndStatus(assignee.getId(), TaskStatus.IN_PROGRESS);
        if (count >= WIP_LIMIT) {
            throw BusinessException.badRequest(
                    "Limite de WIP atingido: " + assignee.getName() + " já possui " + WIP_LIMIT + " tarefas IN_PROGRESS");
        }
    }

    private User resolveAssignee(Long assigneeId, Project project) {
        var assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> BusinessException.notFound("Responsável não encontrado"));
        boolean isMember = project.getMembers().stream().anyMatch(m -> m.getId().equals(assigneeId));
        boolean isOwner = project.getOwner().getId().equals(assigneeId);
        if (!isMember && !isOwner) {
            throw BusinessException.badRequest("Responsável não é membro deste projeto");
        }
        return assignee;
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tarefa não encontrada"));
    }

    private TaskHistory buildHistory(Task task, User user, String field, String oldVal, String newVal) {
        return TaskHistory.builder()
                .task(task).changedBy(user)
                .field(field).oldValue(oldVal).newValue(newVal)
                .build();
    }
}
