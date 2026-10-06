package io.github.tiennnk.problemsolving.task.domain;

import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;

public record TaskCreated(UUID eventId, UUID taskId, ProtocolType protocolType) {

  public static TaskCreated of(Task task) {
    return new TaskCreated(UUID.randomUUID(), task.getId(), task.getProtocolType());
  }
}
