package io.github.tiennnk.problemsolving.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

class JudgeTest {

  private static final CompileResult COMPILED = new CompileResult(true, "", 100);

  private static VerifiedTestCase test(String input, String expected) {
    return new VerifiedTestCase(TestCaseCategory.BASIC, "d", input, expected, expected, true);
  }

  private static RunResult run(int exitCode, String stdout, String stderr, boolean timedOut, boolean truncated) {
    return new RunResult(exitCode, stdout, stderr, 50, timedOut, truncated);
  }

  @Test
  void classifiesEachVerdict() {
    assertThat(Judge.classify(run(0, "0 1\r\n", "", false, false), "0 1")).isEqualTo(Verdict.PASS);
    assertThat(Judge.classify(run(0, "1 0", "", false, false), "0 1")).isEqualTo(Verdict.WRONG_ANSWER);
    assertThat(Judge.classify(run(137, "", "", true, false), "0 1")).isEqualTo(Verdict.TIMEOUT);
    assertThat(Judge.classify(run(137, "", "", false, false), "0 1")).isEqualTo(Verdict.RESOURCE_LIMIT);
    assertThat(Judge.classify(run(1, "", "java.lang.OutOfMemoryError: Java heap space", false, false), "0 1"))
        .isEqualTo(Verdict.RESOURCE_LIMIT);
    assertThat(Judge.classify(run(0, "x".repeat(10), "", false, true), "0 1")).isEqualTo(Verdict.RESOURCE_LIMIT);
    assertThat(Judge.classify(run(1, "", "java.lang.ArrayIndexOutOfBoundsException", false, false), "0 1"))
        .isEqualTo(Verdict.RUNTIME_ERROR);
  }

  @Test
  void passesWhenEveryTestPasses() {
    EvaluationResult result = Judge.evaluate(COMPILED,
        List.of(test("a", "1"), test("b", "2")),
        List.of(run(0, "1\n", "", false, false), run(0, "2\n", "", false, false)));

    assertThat(result.verdict()).isEqualTo(Verdict.PASS);
    assertThat(result.passedTests()).isEqualTo(2);
  }

  @Test
  void reportsFirstFailingTest() {
    EvaluationResult result = Judge.evaluate(COMPILED,
        List.of(test("a", "1"), test("b", "2"), test("c", "3")),
        List.of(run(0, "1", "", false, false), run(0, "5", "", false, false), run(0, "", "", true, false)));

    assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER);
    assertThat(result.reason()).contains("Test #2");
    assertThat(result.passedTests()).isEqualTo(1);
    assertThat(RepairFeedback.from(result)).contains("Expected output:\n2").contains("Your output:\n5");
  }

  @Test
  void compileErrorFailsWithoutRunning() {
    EvaluationResult result = Judge.evaluate(new CompileResult(false, "Main.java:1: error: ';' expected", 100),
        List.of(test("a", "1")), List.of());

    assertThat(result.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
    assertThat(RepairFeedback.from(result)).contains("';' expected");
  }
}
