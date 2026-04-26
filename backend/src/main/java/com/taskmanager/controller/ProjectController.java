package com.taskmanager.controller;

import com.taskmanager.config.AuthHelper;
import com.taskmanager.dto.request.ProjectRequest;
import com.taskmanager.dto.response.ProjectReportResponse;
import com.taskmanager.dto.response.ProjectResponse;
import com.taskmanager.service.ProjectService;
import com.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final AuthHelper authHelper;

    @GetMapping
    @Operation(summary = "List all projects for the current user")
    public List<ProjectResponse> findAll() {
        return projectService.findAll(authHelper.currentUser());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ProjectResponse findById(@PathVariable Long id) {
        return projectService.findById(id, authHelper.currentUser());
    }

    @PostMapping
    @Operation(summary = "Create a new project (ADMIN only)")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(201)
                .body(projectService.create(request, authHelper.currentUser()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project (ADMIN only)")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request, authHelper.currentUser());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id, authHelper.currentUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Get task count report by status and priority")
    public ProjectReportResponse report(@PathVariable Long id) {
        return taskService.getReport(id, authHelper.currentUser());
    }
}
