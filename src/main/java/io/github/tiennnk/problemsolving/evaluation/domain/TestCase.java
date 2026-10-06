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
@Table(name = "test_case")
public class TestCase {

  @Id
  private UUID id;

  @Column(name = "task_id", nullable = false, updatable = false)
  private UUID taskId;

  @Column(name = "position", nullable = false, updatable = false)
  private int position;

  @Column(name = "type", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private TestCaseCategory type;

  @Column(name = "description", nullable = false, updatable = false, columnDefinition = "text")
  private String description;

  @Column(name = "input_data", nullable = false, updatable = false, columnDefinition = "text")
  private String inputData;

  @Column(name = "expected_output", nullable = false, updatable = false, columnDefinition = "text")
  private String expectedOutput;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected TestCase() {
  }

  public static TestCase from(UUID taskId, int position, VerifiedTestCase verified) {
    TestCase testCase = new TestCase();
    testCase.id = UUID.randomUUID();
    testCase.taskId = taskId;
    testCase.position = position;
    testCase.type = verified.category();
    testCase.description = verified.description();
    testCase.inputData = verified.input();
    testCase.expectedOutput = verified.expectedOutput();
    testCase.createdAt = Timestamps.now();
    return testCase;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTaskId() {
    return taskId;
  }

  public int getPosition() {
    return position;
  }

  public TestCaseCategory getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getInputData() {
    return inputData;
  }

  public String getExpectedOutput() {
    return expectedOutput;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
