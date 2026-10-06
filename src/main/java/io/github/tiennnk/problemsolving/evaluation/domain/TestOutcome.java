package io.github.tiennnk.problemsolving.evaluation.domain;

public record TestOutcome(
    int testNumber,
    TestCaseCategory category,
    String description,
    Verdict verdict,
    String input,
    String expectedOutput,
    String actualOutput,
    String stderr,
    long runtimeMs) {
}
