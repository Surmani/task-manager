package com.taskmanager.repository;

import com.taskmanager.domain.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(Long taskId);
}
