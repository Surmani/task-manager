package com.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;
import com.taskmanager.dto.request.ProjectRequest;
import com.taskmanager.dto.request.RegisterRequest;
import com.taskmanager.repository.ProjectRepository;
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
@DisplayName("ProjectController - Integration Tests")
class ProjectControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;

    private String adminToken;
    private String memberToken;
    private User adminUser;

    @BeforeEach
    void setUp() throws Exception {
        projectRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = registerAndGetToken("Admin", "admin@test.com", "123456", UserRole.ADMIN);
        memberToken = registerAndGetToken("Member", "member@test.com", "123456", UserRole.MEMBER);
        adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
    }

    @Test
    @DisplayName("POST /api/projects - ADMIN should create project")
    void create_asAdmin_returns201() throws Exception {
        var request = new ProjectRequest("Project A", "Description", List.of());

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Project A"))
                .andExpect(jsonPath("$.owner.email").value("admin@test.com"));
    }

    @Test
    @DisplayName("POST /api/projects - MEMBER should receive 403")
    void create_asMember_returns403() throws Exception {
        var request = new ProjectRequest("Project A", "Description", List.of());

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/projects - should return only user projects")
    void findAll_returnsOnlyUserProjects() throws Exception {
        var request = new ProjectRequest("My Project", "Desc", List.of());
        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("My Project"));
    }

    @Test
    @DisplayName("GET /api/projects - MEMBER should see empty list when not in any project")
    void findAll_memberNotInProject_returnsEmptyList() throws Exception {
        var request = new ProjectRequest("Admin Project", "Desc", List.of());
        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} - owner should delete project")
    void delete_byOwner_returns204() throws Exception {
        var request = new ProjectRequest("Project to Delete", "Desc", List.of());
        var result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        var projectId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/projects/{id}/report - should return task counters")
    void report_returnsCounters() throws Exception {
        var request = new ProjectRequest("Project Report", "Desc", List.of());
        var result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        var projectId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/projects/" + projectId + "/report")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.TODO").value(0))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.byStatus.DONE").value(0))
                .andExpect(jsonPath("$.byPriority.LOW").value(0));
    }

    @Test
    @DisplayName("GET /api/projects - should return 403 without token")
    void findAll_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isForbidden());
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
