package io.github.tiennnk.problemsolving.worker.domain;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.Timestamps;
import io.github.tiennnk.problemsolving.common.llm.TokenUsage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "worker_run")
public class WorkerRun {

  @Id
  private UUID id;

  @Column(name = "protocol_run_id", nullable = false, updatable = false)
  private UUID protocolRunId;

  @Column(name = "worker_name", nullable = false, updatable = false)
  private String workerName;

  @Column(name = "model")
  private String model;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private WorkerRunStatus status;

  @Column(name = "prompt_tokens", nullable = false)
  private int promptTokens;

  @Column(name = "completion_tokens", nullable = false)
  private int completionTokens;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  protected WorkerRun() {
  }

  public static WorkerRun start(UUID protocolRunId, String workerName) {
    WorkerRun run = new WorkerRun();
    run.id = UUID.randomUUID();
    run.protocolRunId = protocolRunId;
    run.workerName = workerName;
    run.status = WorkerRunStatus.RUNNING;
    run.startedAt = Timestamps.now();
    return run;
  }

  public void recordUsage(String model, TokenUsage usage) {
    this.model = model;
    if (usage != null) {
      promptTokens += usage.promptTokens();
      completionTokens += usage.completionTokens();
    }
  }

  public void finish(WorkerRunStatus finalStatus) {
    if (status != WorkerRunStatus.RUNNING) {
      throw new IllegalStateException("Worker run %s is already %s".formatted(id, status));
    }
    if (finalStatus == WorkerRunStatus.RUNNING) {
      throw new IllegalArgumentException("finalStatus must be SUCCEEDED or FAILED");
    }
    status = finalStatus;
    finishedAt = Timestamps.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getProtocolRunId() {
    return protocolRunId;
  }

  public String getWorkerName() {
    return workerName;
  }

  public String getModel() {
    return model;
  }

  public WorkerRunStatus getStatus() {
    return status;
  }

  public int getPromptTokens() {
    return promptTokens;
  }

  public int getCompletionTokens() {
    return completionTokens;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }
}
