package io.github.tiennnk.problemsolving.task.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;

class TaskTest {

  private Task newTask() {
    return Task.create("Two Sum", Language.JAVA, ProtocolType.PROBLEM_SOLVER);
  }

  private Task inProgressTask() {
    Task task = newTask();
    task.start();
    return task;
  }

  private Task completedTask() {
    Task task = inProgressTask();
    task.complete();
    return task;
  }

  private Task cancelledTask() {
    Task task = newTask();
    task.cancel();
    return task;
  }

  @Test
  void newTaskIsPending() {
    Task task = newTask();

    assertThat(task.getId()).isNotNull();
    assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    assertThat(task.getFailureReason()).isNull();
    assertThat(task.getCreatedAt()).isEqualTo(task.getUpdatedAt());
  }

  @Test
  void createRejectsEmptyProblem() {
    assertThatThrownBy(() -> Task.create("", Language.JAVA, ProtocolType.PROBLEM_SOLVER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRejectsBlankProblem() {
    assertThatThrownBy(() -> Task.create("   ", Language.JAVA, ProtocolType.PROBLEM_SOLVER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRejectsNullLanguage() {
    assertThatThrownBy(() -> Task.create("Two Sum", null, ProtocolType.PROBLEM_SOLVER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRejectsNullProtocolType() {
    assertThatThrownBy(() -> Task.create("Two Sum", Language.JAVA, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void pendingToInProgressToCompleted() {
    Task task = newTask();

    task.start();
    assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

    task.complete();
    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(task.getFailureReason()).isNull();
  }

  @Test
  void failFromPending() {
    Task task = newTask();

    task.fail(FailureReason.INFRASTRUCTURE_ERROR);

    assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
    assertThat(task.getFailureReason()).isEqualTo(FailureReason.INFRASTRUCTURE_ERROR);
  }

  @Test
  void failFromInProgress() {
    Task task = inProgressTask();

    task.fail(FailureReason.NO_PASSING_CANDIDATE);

    assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
    assertThat(task.getFailureReason()).isEqualTo(FailureReason.NO_PASSING_CANDIDATE);
  }

  @Test
  void cancelFromPending() {
    Task task = newTask();

    task.cancel();

    assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
  }

  @Test
  void cancelFromInProgress() {
    Task task = inProgressTask();

    task.cancel();

    assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
  }

  @Test
  void transitionUpdatesUpdatedAt() {
    Task task = newTask();

    task.start();

    assertThat(task.getUpdatedAt()).isAfterOrEqualTo(task.getCreatedAt());
  }

  @Test
  void cannotCompleteFromPending() {
    Task task = newTask();

    assertThatThrownBy(() -> task.complete())
        .isInstanceOf(InvalidTaskTransitionException.class);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
  }

  @Test
  void cannotStartWhenAlreadyInProgress() {
    Task task = inProgressTask();

    assertThatThrownBy(() -> task.start())
        .isInstanceOf(InvalidTaskTransitionException.class);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void cannotStartWhenAlreadyCompleted() {
    Task task = completedTask();

    assertThatThrownBy(() -> task.start())
        .isInstanceOf(InvalidTaskTransitionException.class)
        .hasMessageContaining("COMPLETED");
    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
  }

  @Test
  void cannotCancelWhenCompleted() {
    Task task = completedTask();

    assertThatThrownBy(() -> task.cancel())
        .isInstanceOf(InvalidTaskTransitionException.class);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
  }

  @Test
  void cannotFailWhenCancelled() {
    Task task = cancelledTask();

    assertThatThrownBy(() -> task.fail(FailureReason.TASK_TIMEOUT))
        .isInstanceOf(InvalidTaskTransitionException.class);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    assertThat(task.getFailureReason()).isNull();
  }

  @Test
  void cannotFailTwice() {
    Task task = newTask();
    task.fail(FailureReason.ANALYZER_ERROR);

    assertThatThrownBy(() -> task.fail(FailureReason.TASK_TIMEOUT))
        .isInstanceOf(InvalidTaskTransitionException.class);
    assertThat(task.getFailureReason()).isEqualTo(FailureReason.ANALYZER_ERROR);
  }

  @Test
  void failRequiresReason() {
    Task task = inProgressTask();

    assertThatThrownBy(() -> task.fail(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }
}
