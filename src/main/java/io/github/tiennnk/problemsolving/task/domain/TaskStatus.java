package io.github.tiennnk.problemsolving.task.domain;

public enum TaskStatus {
  PENDING,
  IN_PROGRESS,
  COMPLETED,
  FAILED,
  CANCELLED;

  public boolean isFinal() {
    return this == COMPLETED || this == FAILED || this == CANCELLED;
  }
}