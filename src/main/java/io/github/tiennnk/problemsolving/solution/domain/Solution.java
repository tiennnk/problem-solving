package io.github.tiennnk.problemsolving.solution.domain;

import java.time.Instant;
import java.util.UUID;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.Timestamps;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "solution")
public class Solution {

  @Id
  private UUID id;

  @Column(name = "worker_run_id", nullable = false, updatable = false)
  private UUID workerRunId;

  @Column(name = "version", nullable = false, updatable = false)
  private int version;

  @Column(name = "language", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private Language language;

  @Column(name = "source_code", nullable = false, updatable = false, columnDefinition = "text")
  private String sourceCode;

  @Column(name = "claimed_time_complexity", updatable = false)
  private String claimedTimeComplexity;

  @Column(name = "claimed_space_complexity", updatable = false)
  private String claimedSpaceComplexity;

  @Column(name = "explanation", updatable = false, columnDefinition = "text")
  private String explanation;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Solution() {
  }

  public static Solution from(UUID workerRunId, int version, GeneratedSolution generated) {
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
    Solution solution = new Solution();
    solution.id = UUID.randomUUID();
    solution.workerRunId = workerRunId;
    solution.version = version;
    solution.language = generated.language();
    solution.sourceCode = generated.sourceCode();
    solution.claimedTimeComplexity = abbreviate(generated.claimedTimeComplexity());
    solution.claimedSpaceComplexity = abbreviate(generated.claimedSpaceComplexity());
    solution.explanation = generated.explanation();
    solution.createdAt = Timestamps.now();
    return solution;
  }

  private static String abbreviate(String value) {
    return value == null || value.length() <= 100 ? value : value.substring(0, 100);
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkerRunId() {
    return workerRunId;
  }

  public int getVersion() {
    return version;
  }

  public Language getLanguage() {
    return language;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public String getClaimedTimeComplexity() {
    return claimedTimeComplexity;
  }

  public String getClaimedSpaceComplexity() {
    return claimedSpaceComplexity;
  }

  public String getExplanation() {
    return explanation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
