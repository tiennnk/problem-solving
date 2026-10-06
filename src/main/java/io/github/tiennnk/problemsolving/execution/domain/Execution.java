package io.github.tiennnk.problemsolving.execution.domain;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.Timestamps;
import io.github.tiennnk.problemsolving.evaluation.domain.TestOutcome;
import io.github.tiennnk.problemsolving.evaluation.domain.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution")
public class Execution {

  @Id
  private UUID id;

  @Column(name = "solution_id", nullable = false, updatable = false)
  private UUID solutionId;

  @Column(name = "test_case_id", nullable = false, updatable = false)
  private UUID testCaseId;

  @Column(name = "status", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private Verdict status;

  @Column(name = "stdout", nullable = false, updatable = false, columnDefinition = "text")
  private String stdout;

  @Column(name = "stderr", nullable = false, updatable = false, columnDefinition = "text")
  private String stderr;

  @Column(name = "runtime_ms", nullable = false, updatable = false)
  private long runtimeMs;

  @Column(name = "memory_mb", updatable = false)
  private Integer memoryMb;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Execution() {
  }

  public static Execution from(UUID solutionId, UUID testCaseId, TestOutcome outcome) {
    Execution execution = new Execution();
    execution.id = UUID.randomUUID();
    execution.solutionId = solutionId;
    execution.testCaseId = testCaseId;
    execution.status = outcome.verdict();
    execution.stdout = outcome.actualOutput();
    execution.stderr = outcome.stderr();
    execution.runtimeMs = outcome.runtimeMs();
    execution.createdAt = Timestamps.now();
    return execution;
  }

  public UUID getId() {
    return id;
  }

  public UUID getSolutionId() {
    return solutionId;
  }

  public UUID getTestCaseId() {
    return testCaseId;
  }

  public Verdict getStatus() {
    return status;
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public long getRuntimeMs() {
    return runtimeMs;
  }

  public Integer getMemoryMb() {
    return memoryMb;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
