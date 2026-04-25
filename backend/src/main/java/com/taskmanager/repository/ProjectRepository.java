package com.taskmanager.repository;

import com.taskmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m WHERE p.owner.id = :userId OR m.id = :userId")
    List<Project> findAllByMemberOrOwner(@Param("userId") Long userId);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.members WHERE p.id = :id")
    Optional<Project> findByIdWithMembers(@Param("id") Long id);
}
