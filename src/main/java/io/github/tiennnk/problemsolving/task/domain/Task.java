package io.github.tiennnk.problemsolving.task.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "task")
public class Task {

  @Id
  private UUID id;

  @Column(name = "problem", nullable = false, columnDefinition = "text")
  private String problem;

  @Column(name = "language", nullable = false)
  @Enumerated(EnumType.STRING)
  private Language language;

  @Column(name = "protocol_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ProtocolType protocolType;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private TaskStatus status;

  @Column(name = "failure_reason")
  @Enumerated(EnumType.STRING)
  private FailureReason failureReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private Long version;

  protected Task() {
  }

  public static Task create(String problem, Language language, ProtocolType protocolType) {
    if (problem == null || problem.isBlank()) {
      throw new IllegalArgumentException("problem must not be blank");
    }

    if (language == null) {
      throw new IllegalArgumentException("language must not be null");
    }

    if (protocolType == null) {
      throw new IllegalArgumentException("protocolType must not be null");
    }

    Task task = new Task();
    Instant now = now();

    task.id = UUID.randomUUID();
    task.problem = problem;
    task.language = language;
    task.protocolType = protocolType;
    task.status = TaskStatus.PENDING;
    task.createdAt = now;
    task.updatedAt = now;

    return task;
  }

  private void requireStatus(TaskStatus target, TaskStatus... allowedFrom) {
    boolean allowed = false;

    for (TaskStatus allowedStatus : allowedFrom) {
      if (this.status == allowedStatus) {
        allowed = true;
        break;
      }
    }

    if (!allowed) {
      throw new InvalidTaskTransitionException(id, this.status, target);
    }
  }

  private void changeStatus(TaskStatus target) {
    this.status = target;
    this.updatedAt = now();
  }

  private static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  public void start() {
    requireStatus(TaskStatus.IN_PROGRESS, TaskStatus.PENDING);
    changeStatus(TaskStatus.IN_PROGRESS);
  }

  public void complete() {
    requireStatus(TaskStatus.COMPLETED, TaskStatus.IN_PROGRESS);
    changeStatus(TaskStatus.COMPLETED);
  }

  public void cancel() {
    requireStatus(TaskStatus.CANCELLED, TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    changeStatus(TaskStatus.CANCELLED);
  }

  public void fail(FailureReason failureReason) {
    if (failureReason == null) {
      throw new IllegalArgumentException("failureReason must not be null");
    }
    requireStatus(TaskStatus.FAILED, TaskStatus.IN_PROGRESS, TaskStatus.PENDING);
    this.failureReason = failureReason;
    changeStatus(TaskStatus.FAILED);
  }

  public UUID getId() {
    return id;
  }

  public String getProblem() {
    return problem;
  }

  public Language getLanguage() {
    return language;
  }

  public ProtocolType getProtocolType() {
    return protocolType;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public FailureReason getFailureReason() {
    return failureReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Long getVersion() {
    return version;
  }
}
