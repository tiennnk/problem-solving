package io.github.tiennnk.problemsolving.task.application;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.domain.TaskCreated;
import io.github.tiennnk.problemsolving.task.infrastructure.TaskRepository;

@Service
public class TaskService {

  private static final ProtocolType DEFAULT_PROTOCOL = ProtocolType.PROBLEM_SOLVER;

  private final TaskRepository taskRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TaskService(TaskRepository taskRepository, ApplicationEventPublisher eventPublisher) {
    this.taskRepository = taskRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Task createTask(CreateTaskCommand command) {
    ProtocolType protocolType = command.protocolType() != null
        ? command.protocolType()
        : DEFAULT_PROTOCOL;
    Task task = taskRepository.save(Task.create(command.problem(), command.language(), protocolType));
    eventPublisher.publishEvent(TaskCreated.of(task));
    return task;
  }

  @Transactional(readOnly = true)
  public Task getTask(UUID taskId) {
    return taskRepository.findById(taskId)
        .orElseThrow(() -> new TaskNotFoundException(taskId));
  }

  @Transactional
  public Task startTask(UUID taskId) {
    Task task = getTask(taskId);
    task.start();
    return task;
  }

  @Transactional
  public void completeTask(UUID taskId) {
    getTask(taskId).complete();
  }

  @Transactional
  public void failTask(UUID taskId, FailureReason reason) {
    getTask(taskId).fail(reason);
  }
}
