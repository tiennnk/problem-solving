package io.github.tiennnk.problemsolving.evaluation.domain;

import java.util.List;

import io.github.tiennnk.problemsolving.common.llm.TokenUsage;

public record GeneratedTestSuite(
    String inputFormat,
    String outputFormat,
    List<GeneratedTestCase> testCases,
    String model,
    TokenUsage tokenUsage) {

  public GeneratedTestSuite {
    testCases = List.copyOf(testCases);
  }

  public String ioSpecification() {
    return "Input: " + inputFormat + "\nOutput: " + outputFormat;
  }
}
