package com.taskmanager.repository;

import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :text, '%')))")
    Page<Task> searchByText(@Param("projectId") Long projectId,
                            @Param("text") String text,
                            Pageable pageable);
}
