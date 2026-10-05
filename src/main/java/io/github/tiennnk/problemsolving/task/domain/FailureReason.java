package io.github.tiennnk.problemsolving.task.domain;

public enum FailureReason {
  TASK_TIMEOUT,
  NO_PASSING_CANDIDATE,
  REPAIR_EXHAUSTED,
  ANALYZER_ERROR,
  INFRASTRUCTURE_ERROR,
}
