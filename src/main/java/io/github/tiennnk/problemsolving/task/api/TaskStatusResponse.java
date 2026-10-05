package io.github.tiennnk.problemsolving.task.api;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.domain.TaskStatus;

public record TaskStatusResponse(
    UUID id,
    TaskStatus status,
    FailureReason failureReason,
    Instant updatedAt) {

  static TaskStatusResponse from(Task task) {
    return new TaskStatusResponse(
        task.getId(),
        task.getStatus(),
        task.getFailureReason(),
        task.getUpdatedAt());
  }
}
