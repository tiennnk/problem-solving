package io.github.tiennnk.problemsolving.task.domain;

import java.util.UUID;

public class InvalidTaskTransitionException extends RuntimeException {
  public InvalidTaskTransitionException(UUID taskId, TaskStatus from, TaskStatus to) {
    super(String.format("Invalid task transition from %s to %s for task with id %s", from, to, taskId));
  }

}
