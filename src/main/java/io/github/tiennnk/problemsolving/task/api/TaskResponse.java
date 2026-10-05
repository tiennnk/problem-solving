package io.github.tiennnk.problemsolving.task.api;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.domain.TaskStatus;

public record TaskResponse(
    UUID id,
    String problem,
    Language language,
    ProtocolType protocolType,
    TaskStatus status,
    FailureReason failureReason,
    Instant createdAt,
    Instant updatedAt) {

  static TaskResponse from(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getProblem(),
        task.getLanguage(),
        task.getProtocolType(),
        task.getStatus(),
        task.getFailureReason(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
