package io.github.tiennnk.problemsolving.evaluation.domain;

import java.util.ArrayList;
import java.util.List;

import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

public final class Judge {

  static final int MAX_STORED_TEXT = 2_000;
  private static final int KILLED_BY_SIGKILL = 137;

  private Judge() {
  }

  public static EvaluationResult evaluate(CompileResult compile, List<VerifiedTestCase> tests, List<RunResult> runs) {
    if (!compile.success()) {
      return new EvaluationResult(Verdict.COMPILE_ERROR, "Compilation failed", truncate(compile.output()), List.of());
    }
    if (tests.size() != runs.size()) {
      throw new IllegalArgumentException("Expected one run per test: %d tests, %d runs"
          .formatted(tests.size(), runs.size()));
    }

    List<TestOutcome> outcomes = new ArrayList<>();
    for (int i = 0; i < tests.size(); i++) {
      outcomes.add(judgeTest(i + 1, tests.get(i), runs.get(i)));
    }

    return outcomes.stream()
        .filter(outcome -> outcome.verdict() != Verdict.PASS)
        .findFirst()
        .map(failure -> new EvaluationResult(failure.verdict(), describe(failure), truncate(compile.output()), outcomes))
        .orElseGet(() -> new EvaluationResult(Verdict.PASS, "All %d tests passed".formatted(outcomes.size()),
            truncate(compile.output()), outcomes));
  }

  static Verdict classify(RunResult run, String expectedOutput) {
    if (run.timedOut()) {
      return Verdict.TIMEOUT;
    }
    if (run.outputTruncated() || isOutOfMemory(run)) {
      return Verdict.RESOURCE_LIMIT;
    }
    if (run.exitCode() != 0) {
      return Verdict.RUNTIME_ERROR;
    }
    return OutputNormalizer.sameOutput(run.stdout(), expectedOutput) ? Verdict.PASS : Verdict.WRONG_ANSWER;
  }

  private static boolean isOutOfMemory(RunResult run) {
    String stderr = run.stderr() == null ? "" : run.stderr();
    return run.exitCode() == KILLED_BY_SIGKILL
        || stderr.contains("java.lang.OutOfMemoryError")
        || stderr.contains("Allowed memory size of");
  }

  private static TestOutcome judgeTest(int testNumber, VerifiedTestCase test, RunResult run) {
    return new TestOutcome(
        testNumber,
        test.category(),
        test.description(),
        classify(run, test.expectedOutput()),
        truncate(test.input()),
        test.expectedOutput(),
        truncate(OutputNormalizer.normalize(run.stdout())),
        truncate(run.stderr()),
        run.durationMs());
  }

  private static String describe(TestOutcome failure) {
    return "Test #%d (%s%s) failed with %s".formatted(
        failure.testNumber(),
        failure.category(),
        failure.description().isBlank() ? "" : ": " + failure.description(),
        failure.verdict());
  }

  static String truncate(String text) {
    if (text == null) {
      return "";
    }
    return text.length() <= MAX_STORED_TEXT ? text : text.substring(0, MAX_STORED_TEXT) + "\n...[truncated]";
  }
}
