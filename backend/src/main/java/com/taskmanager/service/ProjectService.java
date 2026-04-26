package com.taskmanager.service;

import com.taskmanager.domain.entity.Project;
import com.taskmanager.domain.entity.User;
import com.taskmanager.dto.request.ProjectRequest;
import com.taskmanager.dto.response.ProjectResponse;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.repository.ProjectRepository;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public List<ProjectResponse> findAll(User currentUser) {
        return projectRepository.findAllByMemberOrOwner(currentUser.getId())
                .stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse findById(Long id, User currentUser) {
        var project = getProjectAndCheckAccess(id, currentUser);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, User currentUser) {
        var members = resolveMembers(request.memberIds());
        var project = Project.builder()
                .name(request.name())
                .description(request.description())
                .owner(currentUser)
                .members(members)
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request, User currentUser) {
        var project = getProjectAndCheckOwner(id, currentUser);
        project.setName(request.name());
        project.setDescription(request.description());
        if (request.memberIds() != null) {
            project.setMembers(resolveMembers(request.memberIds()));
        }
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        var project = getProjectAndCheckOwner(id, currentUser);
        projectRepository.delete(project);
    }

    public Project getProjectAndCheckAccess(Long projectId, User user) {
        var project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> BusinessException.notFound("Projeto não encontrado"));
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isOwner && !isMember) {
            throw BusinessException.forbidden("Você não pertence a este projeto");
        }
        return project;
    }

    public Project getProjectAndCheckOwner(Long projectId, User user) {
        var project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> BusinessException.notFound("Projeto não encontrado"));
        if (!project.getOwner().getId().equals(user.getId())) {
            throw BusinessException.forbidden("Apenas o dono pode realizar esta ação");
        }
        return project;
    }

    private List<User> resolveMembers(List<Long> memberIds) {
        if (memberIds == null) return new ArrayList<>();
        return userRepository.findAllById(memberIds);
    }
}
