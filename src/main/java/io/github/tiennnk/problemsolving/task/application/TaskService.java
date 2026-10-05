package io.github.tiennnk.problemsolving.task.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.infrastructure.TaskRepository;

@Service
public class TaskService {

  private static final ProtocolType DEFAULT_PROTOCOL = ProtocolType.PROBLEM_SOLVER;

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional
  public Task createTask(CreateTaskCommand command) {
    ProtocolType protocolType = command.protocolType() != null
        ? command.protocolType()
        : DEFAULT_PROTOCOL;
    Task task = Task.create(command.problem(), command.language(), protocolType);
    return taskRepository.save(task);
  }

  @Transactional(readOnly = true)
  public Task getTask(UUID taskId) {
    return taskRepository.findById(taskId)
        .orElseThrow(() -> new TaskNotFoundException(taskId));
  }
}
