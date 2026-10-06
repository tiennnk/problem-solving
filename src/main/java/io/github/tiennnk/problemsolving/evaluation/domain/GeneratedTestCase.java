package io.github.tiennnk.problemsolving.evaluation.domain;

public record GeneratedTestCase(
    TestCaseCategory category,
    String description,
    String input,
    String claimedOutput) {
}
