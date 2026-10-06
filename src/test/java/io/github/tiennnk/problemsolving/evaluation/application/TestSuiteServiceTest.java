package io.github.tiennnk.problemsolving.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.tiennnk.problemsolving.common.llm.TokenUsage;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestCase;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestSuite;
import io.github.tiennnk.problemsolving.evaluation.domain.TestCaseCategory;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import io.github.tiennnk.problemsolving.execution.application.CodeSandbox;
import io.github.tiennnk.problemsolving.execution.application.SandboxSession;
import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

class TestSuiteServiceTest {

  private static GeneratedTestSuite generatedSuite(GeneratedTestCase... tests) {
    return new GeneratedTestSuite("in", "out", "public class Main {}", List.of(tests), "m", TokenUsage.UNKNOWN);
  }

  private static GeneratedTestCase test(String input, String claimed) {
    return new GeneratedTestCase(TestCaseCategory.BASIC, "d", input, claimed);
  }

  private static CodeSandbox sandbox(boolean compiles, Map<String, RunResult> runs) {
    return (language, source) -> new SandboxSession() {
      @Override
      public CompileResult compile() {
        return new CompileResult(compiles, compiles ? "" : "error: ';' expected", 1);
      }

      @Override
      public RunResult run(String stdin) {
        return runs.get(stdin);
      }

      @Override
      public void close() {
      }
    };
  }

  private static RunResult ok(String stdout) {
    return new RunResult(0, stdout, "", 10, false, false);
  }

  @Test
  void usesReferenceOutputAsExpectedAndFlagsWrongClaims() {
    GeneratedTestSuite generated = generatedSuite(
        test("a", "0 1"), test("b", "0 3"), test("c", "2 3"));
    CodeSandbox sandbox = sandbox(true, Map.of("a", ok("0 1\n"), "b", ok("1 3\n"), "c", ok("2 3\n")));

    VerifiedTestSuite suite = new TestSuiteService(problem -> generated, sandbox).prepare("Two Sum");

    assertThat(suite.testCases()).hasSize(3);
    assertThat(suite.testCases().get(1).expectedOutput()).isEqualTo("1 3");
    assertThat(suite.testCases().get(1).claimMatchesReference()).isFalse();
    assertThat(suite.disagreements()).isEqualTo(1);
  }

  @Test
  void dropsTestsWhereReferenceFails() {
    GeneratedTestSuite generated = generatedSuite(
        test("a", "1"), test("b", "2"), test("c", "3"), test("d", "4"));
    RunResult timeout = new RunResult(137, "", "", 5000, true, false);
    CodeSandbox sandbox = sandbox(true, Map.of("a", ok("1"), "b", ok("2"), "c", ok("3"), "d", timeout));

    VerifiedTestSuite suite = new TestSuiteService(problem -> generated, sandbox).prepare("p");

    assertThat(suite.testCases()).hasSize(3);
    assertThat(suite.droppedTests()).isEqualTo(1);
  }

  @Test
  void failsWhenReferenceDoesNotCompile() {
    GeneratedTestSuite generated = generatedSuite(test("a", "1"), test("b", "2"), test("c", "3"));

    assertThatThrownBy(() -> new TestSuiteService(problem -> generated, sandbox(false, Map.of())).prepare("p"))
        .isInstanceOf(TestSuitePreparationException.class)
        .hasMessageContaining("does not compile");
  }
}
