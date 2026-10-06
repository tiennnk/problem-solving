package io.github.tiennnk.problemsolving.evaluation.domain;

import java.util.List;
import java.util.Optional;

public record EvaluationResult(
    Verdict verdict,
    String reason,
    String compilerOutput,
    List<TestOutcome> outcomes) {

  public EvaluationResult {
    outcomes = List.copyOf(outcomes);
  }

  public boolean passed() {
    return verdict == Verdict.PASS;
  }

  public long passedTests() {
    return outcomes.stream().filter(outcome -> outcome.verdict() == Verdict.PASS).count();
  }

  public int totalTests() {
    return outcomes.size();
  }

  public long maxRuntimeMs() {
    return outcomes.stream().mapToLong(TestOutcome::runtimeMs).max().orElse(0);
  }

  public Optional<TestOutcome> firstFailure() {
    return outcomes.stream().filter(outcome -> outcome.verdict() != Verdict.PASS).findFirst();
  }
}
