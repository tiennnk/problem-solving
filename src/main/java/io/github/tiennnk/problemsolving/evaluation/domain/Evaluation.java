package io.github.tiennnk.problemsolving.evaluation.domain;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.Timestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluation")
public class Evaluation {

  @Id
  private UUID id;

  @Column(name = "solution_id", nullable = false, updatable = false)
  private UUID solutionId;

  @Column(name = "status", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private Verdict status;

  @Column(name = "reason", nullable = false, updatable = false, columnDefinition = "text")
  private String reason;

  @Column(name = "compiler_output", nullable = false, updatable = false, columnDefinition = "text")
  private String compilerOutput;

  @Column(name = "passed_tests", nullable = false, updatable = false)
  private int passedTests;

  @Column(name = "total_tests", nullable = false, updatable = false)
  private int totalTests;

  @Column(name = "max_runtime_ms", nullable = false, updatable = false)
  private long maxRuntimeMs;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Evaluation() {
  }

  public static Evaluation from(UUID solutionId, EvaluationResult result, int totalTests) {
    Evaluation evaluation = new Evaluation();
    evaluation.id = UUID.randomUUID();
    evaluation.solutionId = solutionId;
    evaluation.status = result.verdict();
    evaluation.reason = result.reason();
    evaluation.compilerOutput = result.compilerOutput();
    evaluation.passedTests = (int) result.passedTests();
    evaluation.totalTests = totalTests;
    evaluation.maxRuntimeMs = result.maxRuntimeMs();
    evaluation.createdAt = Timestamps.now();
    return evaluation;
  }

  public UUID getId() {
    return id;
  }

  public UUID getSolutionId() {
    return solutionId;
  }

  public Verdict getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }

  public String getCompilerOutput() {
    return compilerOutput;
  }

  public int getPassedTests() {
    return passedTests;
  }

  public int getTotalTests() {
    return totalTests;
  }

  public long getMaxRuntimeMs() {
    return maxRuntimeMs;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
