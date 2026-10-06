package io.github.tiennnk.problemsolving.evaluation.domain;

import java.util.List;

import io.github.tiennnk.problemsolving.common.llm.TokenUsage;

public record VerifiedTestSuite(
    String inputFormat,
    String outputFormat,
    String referenceSolution,
    List<VerifiedTestCase> testCases,
    int droppedTests,
    String model,
    TokenUsage tokenUsage) {

  public VerifiedTestSuite {
    testCases = List.copyOf(testCases);
  }

  public String ioSpecification() {
    return "Input: " + inputFormat + "\nOutput: " + outputFormat;
  }

  public long disagreements() {
    return testCases.stream().filter(test -> !test.claimMatchesReference()).count();
  }
}
