package com.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.domain.enums.Priority;
import com.taskmanager.domain.enums.UserRole;
import com.taskmanager.dto.request.ProjectRequest;
import com.taskmanager.dto.request.RegisterRequest;
import com.taskmanager.dto.request.TaskRequest;
import com.taskmanager.dto.request.TaskUpdateRequest;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TaskController - Integration Tests")
class TaskControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;

    private String adminToken;
    private String memberToken;
    private Long projectId;
    private Long adminId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = registerAndGetToken("Admin", "admin@test.com", "123456", UserRole.ADMIN);
        memberToken = registerAndGetToken("Member", "member@test.com", "123456", UserRole.MEMBER);
        adminId = userRepository.findByEmail("admin@test.com").orElseThrow().getId();

        var projectResult = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProjectRequest("Test Project", "Desc", List.of()))))
                .andReturn();
        projectId = objectMapper.readTree(
                projectResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/projects/{id}/tasks - should create task with TODO status")
    void create_validRequest_returns201WithTodoStatus() throws Exception {
        var request = new TaskRequest("New Task", "Description", Priority.HIGH, null, null);

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Task"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/tasks - should return paginated tasks")
    void findAll_returnsPaginatedTasks() throws Exception {
        createTask("Task 1", Priority.HIGH);
        createTask("Task 2", Priority.LOW);

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/tasks - should filter by status")
    void findAll_filterByStatus_returnsFiltered() throws Exception {
        createTask("Task TODO", Priority.MEDIUM);

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks")
                        .param("status", "TODO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks")
                        .param("status", "DONE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/tasks/search - should find tasks by text")
    void search_byText_returnsMatchingTasks() throws Exception {
        createTask("Fix login bug", Priority.HIGH);
        createTask("Create dashboard", Priority.MEDIUM);

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks/search")
                        .param("q", "bug")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix login bug"));
    }

    @Test
    @DisplayName("PATCH /api/projects/{id}/tasks/{taskId} - DONE cannot go back to TODO")
    void update_doneToTodo_returns400() throws Exception {
        var taskId = createTask("Task", Priority.MEDIUM);

        // Move to DONE
        mockMvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new TaskUpdateRequest(null, null,
                                com.taskmanager.domain.enums.TaskStatus.DONE,
                                null, null, null, null))));

        // Try to move back to TODO
        mockMvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskUpdateRequest(null, null,
                                        com.taskmanager.domain.enums.TaskStatus.TODO,
                                        null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A DONE task cannot go back to TODO"));
    }

    @Test
    @DisplayName("GET /api/projects/{id}/tasks/{taskId}/history - should return audit log")
    void history_returnsChangeLog() throws Exception {
        var taskId = createTask("Task with history", Priority.LOW);

        mockMvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new TaskUpdateRequest("Updated Title", null,
                                null, null, null, null, null))));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks/" + taskId + "/history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].field").value("title"))
                .andExpect(jsonPath("$[0].oldValue").value("Task with history"))
                .andExpect(jsonPath("$[0].newValue").value("Updated Title"));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id}/tasks/{taskId} - should delete task")
    void delete_existingTask_returns204() throws Exception {
        var taskId = createTask("Task to Delete", Priority.LOW);

        mockMvc.perform(delete("/api/projects/" + projectId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/projects/{id}/tasks - MEMBER not in project gets 403")
    void create_memberNotInProject_returns403() throws Exception {
        var request = new TaskRequest("Task", "Desc", Priority.LOW, null, null);

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Long createTask(String title, Priority priority) throws Exception {
        var request = new TaskRequest(title, "Description", priority, null, null);
        var result = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registerAndGetToken(String name, String email,
                                       String password, UserRole role) throws Exception {
        var request = new RegisterRequest(name, email, password, role);
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readTree(
                result.getResponse().getContentAsString()).get("token").asText();
    }
}
