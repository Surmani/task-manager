package com.taskmanager.controller;

import com.taskmanager.config.AuthHelper;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.TaskStatus;
import com.taskmanager.dto.request.TaskRequest;
import com.taskmanager.dto.request.TaskUpdateRequest;
import com.taskmanager.dto.response.PageResponse;
import com.taskmanager.dto.response.TaskHistoryResponse;
import com.taskmanager.dto.response.TaskResponse;
import com.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;
    private final AuthHelper authHelper;

    @GetMapping
    @Operation(summary = "List tasks with filters, sorting and pagination")
    public PageResponse<TaskResponse> findAll(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return taskService.findAll(projectId, authHelper.currentUser(),
                status, priority, assigneeId, from, to, sortBy, page, size);
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search on title and description")
    public PageResponse<TaskResponse> search(
            @PathVariable Long projectId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return taskService.search(projectId, q, authHelper.currentUser(), page, size);
    }

    @GetMapping("/{taskId}/history")
    @Operation(summary = "Get task change history (audit log)")
    public List<TaskHistoryResponse> history(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        return taskService.getHistory(taskId, authHelper.currentUser());
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(201)
                .body(taskService.create(projectId, request, authHelper.currentUser()));
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Partially update a task")
    public TaskResponse update(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request) {
        return taskService.update(taskId, request, authHelper.currentUser());
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        taskService.delete(taskId, authHelper.currentUser());
        return ResponseEntity.noContent().build();
    }
}
