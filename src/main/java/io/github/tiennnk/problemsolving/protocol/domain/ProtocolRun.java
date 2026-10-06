package io.github.tiennnk.problemsolving.protocol.domain;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.common.domain.Timestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "protocol_run")
public class ProtocolRun {

  public static final int MAX_REPAIR_ATTEMPTS = 2;

  @Id
  private UUID id;

  @Column(name = "task_id", nullable = false, updatable = false)
  private UUID taskId;

  @Column(name = "protocol_type", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private ProtocolType protocolType;

  @Column(name = "current_state", nullable = false)
  @Enumerated(EnumType.STRING)
  private ProblemSolverState currentState;

  @Column(name = "attempt", nullable = false)
  private int attempt;

  @Column(name = "failure_detail", columnDefinition = "text")
  private String failureDetail;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Version
  private Long version;

  protected ProtocolRun() {
  }

  public static ProtocolRun start(UUID taskId, ProtocolType protocolType) {
    if (taskId == null || protocolType == null) {
      throw new IllegalArgumentException("taskId and protocolType must not be null");
    }
    ProtocolRun run = new ProtocolRun();
    run.id = UUID.randomUUID();
    run.taskId = taskId;
    run.protocolType = protocolType;
    run.currentState = ProblemSolverState.ANALYZE;
    run.attempt = 0;
    run.startedAt = Timestamps.now();
    return run;
  }

  public void transitionTo(ProblemSolverState target) {
    if (target == ProblemSolverState.REPAIR && !canRepair()) {
      throw new InvalidProtocolTransitionException(id, currentState, target);
    }
    if (!currentState.canTransitionTo(target)) {
      throw new InvalidProtocolTransitionException(id, currentState, target);
    }
    if (target == ProblemSolverState.REPAIR) {
      attempt++;
    }
    currentState = target;
    if (target.isTerminal()) {
      completedAt = Timestamps.now();
    }
  }

  public void fail(String detail) {
    transitionTo(ProblemSolverState.FAILED);
    this.failureDetail = detail;
  }

  public boolean canRepair() {
    return attempt < MAX_REPAIR_ATTEMPTS;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTaskId() {
    return taskId;
  }

  public ProtocolType getProtocolType() {
    return protocolType;
  }

  public ProblemSolverState getCurrentState() {
    return currentState;
  }

  public int getAttempt() {
    return attempt;
  }

  public String getFailureDetail() {
    return failureDetail;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Long getVersion() {
    return version;
  }
}
