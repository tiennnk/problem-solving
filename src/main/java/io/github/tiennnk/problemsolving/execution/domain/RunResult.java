package io.github.tiennnk.problemsolving.execution.domain;

public record RunResult(
    int exitCode,
    String stdout,
    String stderr,
    long durationMs,
    boolean timedOut,
    boolean outputTruncated) {
}
