package io.github.tiennnk.problemsolving.evaluation.application;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.evaluation.domain.EvaluationResult;
import io.github.tiennnk.problemsolving.evaluation.domain.Judge;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestCase;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import io.github.tiennnk.problemsolving.execution.application.CodeSandbox;
import io.github.tiennnk.problemsolving.execution.application.SandboxSession;
import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

@Service
public class SolutionEvaluator {

  private final CodeSandbox codeSandbox;

  public SolutionEvaluator(CodeSandbox codeSandbox) {
    this.codeSandbox = codeSandbox;
  }

  public EvaluationResult evaluate(Language language, String sourceCode, VerifiedTestSuite suite) {
    List<VerifiedTestCase> tests = suite.testCases();
    try (SandboxSession session = codeSandbox.start(language, sourceCode)) {
      CompileResult compile = session.compile();
      if (!compile.success()) {
        return Judge.evaluate(compile, tests, List.of());
      }
      List<RunResult> runs = tests.stream().map(test -> session.run(test.input())).toList();
      return Judge.evaluate(compile, tests, runs);
    }
  }
}
