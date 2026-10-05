package io.github.tiennnk.problemsolving.task.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.tiennnk.problemsolving.task.domain.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {
}
