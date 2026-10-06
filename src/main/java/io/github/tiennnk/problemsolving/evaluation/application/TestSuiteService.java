package io.github.tiennnk.problemsolving.evaluation.application;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestCase;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestSuite;
import io.github.tiennnk.problemsolving.evaluation.domain.OutputNormalizer;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestCase;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import io.github.tiennnk.problemsolving.execution.application.CodeSandbox;
import io.github.tiennnk.problemsolving.execution.application.SandboxSession;
import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

@Service
public class TestSuiteService {

  static final int MIN_VERIFIED_TESTS = 3;

  private static final Logger log = LoggerFactory.getLogger(TestSuiteService.class);

  private final TestGenerator testGenerator;
  private final CodeSandbox codeSandbox;

  public TestSuiteService(TestGenerator testGenerator, CodeSandbox codeSandbox) {
    this.testGenerator = testGenerator;
    this.codeSandbox = codeSandbox;
  }

  public VerifiedTestSuite prepare(String problem) {
    GeneratedTestSuite generated = testGenerator.generate(problem);

    List<VerifiedTestCase> verified = new ArrayList<>();
    try (SandboxSession session = codeSandbox.start(Language.JAVA, generated.referenceSolution())) {
      CompileResult compile = session.compile();
      if (!compile.success()) {
        throw new TestSuitePreparationException("Reference solution does not compile: " + compile.output());
      }
      for (GeneratedTestCase test : generated.testCases()) {
        RunResult run = session.run(test.input());
        if (run.timedOut() || run.exitCode() != 0 || run.outputTruncated()) {
          log.warn("Dropping test '{}': reference solution failed (exit {}, timedOut {})",
              test.description(), run.exitCode(), run.timedOut());
          continue;
        }
        String expected = OutputNormalizer.normalize(run.stdout());
        if (expected.isEmpty()) {
          log.warn("Dropping test '{}': reference solution printed nothing", test.description());
          continue;
        }
        verified.add(new VerifiedTestCase(
            test.category(),
            test.description(),
            test.input(),
            expected,
            test.claimedOutput(),
            OutputNormalizer.sameOutput(test.claimedOutput(), expected)));
      }
    }

    if (verified.size() < MIN_VERIFIED_TESTS) {
      throw new TestSuitePreparationException("Only %d of %d tests could be verified by the reference solution"
          .formatted(verified.size(), generated.testCases().size()));
    }
    return new VerifiedTestSuite(
        generated.inputFormat(),
        generated.outputFormat(),
        generated.referenceSolution(),
        verified,
        generated.testCases().size() - verified.size(),
        generated.model(),
        generated.tokenUsage());
  }
}
