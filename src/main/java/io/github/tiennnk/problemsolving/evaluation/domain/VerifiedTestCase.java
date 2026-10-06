package io.github.tiennnk.problemsolving.evaluation.domain;

public record VerifiedTestCase(
    TestCaseCategory category,
    String description,
    String input,
    String expectedOutput,
    String claimedOutput,
    boolean claimMatchesReference) {
}
