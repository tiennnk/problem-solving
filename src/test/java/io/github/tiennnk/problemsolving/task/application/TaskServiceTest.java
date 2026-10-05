package io.github.tiennnk.problemsolving.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.domain.TaskStatus;
import io.github.tiennnk.problemsolving.task.infrastructure.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  TaskRepository taskRepository;

  @InjectMocks
  TaskService taskService;

  @Test
  void createTaskSavesPendingTask() {
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task task = taskService.createTask(
        new CreateTaskCommand("Two Sum", Language.JAVA, ProtocolType.PROBLEM_SOLVER));

    assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    assertThat(task.getProblem()).isEqualTo("Two Sum");
    assertThat(task.getLanguage()).isEqualTo(Language.JAVA);
    verify(taskRepository).save(task);
  }

  @Test
  void createTaskDefaultsProtocolTypeWhenMissing() {
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Task task = taskService.createTask(new CreateTaskCommand("Two Sum", Language.JAVA, null));

    assertThat(task.getProtocolType()).isEqualTo(ProtocolType.PROBLEM_SOLVER);
  }

  @Test
  void createTaskDoesNotSaveInvalidTask() {
    assertThatThrownBy(() -> taskService.createTask(
        new CreateTaskCommand("  ", Language.JAVA, null)))
        .isInstanceOf(IllegalArgumentException.class);

    verify(taskRepository, never()).save(any());
  }

  @Test
  void getTaskReturnsExistingTask() {
    Task existing = Task.create("Two Sum", Language.JAVA, ProtocolType.PROBLEM_SOLVER);
    when(taskRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

    assertThat(taskService.getTask(existing.getId())).isSameAs(existing);
  }

  @Test
  void getTaskThrowsWhenMissing() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.getTask(id))
        .isInstanceOf(TaskNotFoundException.class)
        .hasMessageContaining(id.toString());
  }
}
