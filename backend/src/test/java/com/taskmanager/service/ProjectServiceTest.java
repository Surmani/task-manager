package com.taskmanager.service;

import com.taskmanager.domain.entity.Project;
import com.taskmanager.domain.entity.User;
import com.taskmanager.domain.enums.UserRole;
import com.taskmanager.dto.request.ProjectRequest;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
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
@DisplayName("ProjectService - Unit Tests")
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ProjectService projectService;

    private User owner;
    private User member;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner")
                .email("owner@test.com").role(UserRole.ADMIN).build();
        member = User.builder().id(2L).name("Member")
                .email("member@test.com").role(UserRole.MEMBER).build();
        project = Project.builder()
                .id(1L).name("Project")
                .owner(owner)
                .members(new ArrayList<>(List.of(member)))
                .tasks(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should create project and return ProjectResponse")
    void create_validRequest_returnsProjectResponse() {
        var request = new ProjectRequest("New Project", "Description", List.of());
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return Project.builder()
                    .id(1L).name(p.getName())
                    .description(p.getDescription())
                    .owner(owner)
                    .members(new ArrayList<>())
                    .tasks(new ArrayList<>())
                    .build();
        });

        var response = projectService.create(request, owner);

        assertThat(response.name()).isEqualTo("New Project");
        assertThat(response.owner().email()).isEqualTo("owner@test.com");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should throw forbidden when user does not belong to project")
    void getProjectAndCheckAccess_userNotMember_throwsForbidden() {
        var outsider = User.builder().id(99L).name("Outsider").build();
        when(projectRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getProjectAndCheckAccess(1L, outsider))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not belong");
    }

    @Test
    @DisplayName("Should allow owner to access project")
    void getProjectAndCheckAccess_owner_succeeds() {
        when(projectRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(project));

        assertThatNoException().isThrownBy(() ->
                projectService.getProjectAndCheckAccess(1L, owner));
    }

    @Test
    @DisplayName("Should allow member to access project")
    void getProjectAndCheckAccess_member_succeeds() {
        when(projectRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(project));

        assertThatNoException().isThrownBy(() ->
                projectService.getProjectAndCheckAccess(1L, member));
    }

    @Test
    @DisplayName("Should throw forbidden when non-owner tries to delete")
    void delete_byMember_throwsForbidden() {
        when(projectRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.delete(1L, member))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("Should throw not found when project does not exist")
    void findById_notFound_throwsBusinessException() {
        when(projectRepository.findByIdWithMembers(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectAndCheckAccess(99L, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should allow owner to delete project")
    void delete_byOwner_succeeds() {
        when(projectRepository.findByIdWithMembers(1L)).thenReturn(Optional.of(project));

        assertThatNoException().isThrownBy(() -> projectService.delete(1L, owner));
        verify(projectRepository).delete(project);
    }
}
