package io.github.tiennnk.problemsolving.protocol.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.evaluation.application.SolutionEvaluator;
import io.github.tiennnk.problemsolving.evaluation.application.TestSuiteService;
import io.github.tiennnk.problemsolving.evaluation.domain.EvaluationResult;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import io.github.tiennnk.problemsolving.worker.application.CodingWorker;
import io.github.tiennnk.problemsolving.worker.domain.CodeGenerationRequest;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Profile("dev")
@RestController
@RequestMapping("/api/dev")
public class SolvePlaygroundController {

  private final TestSuiteService testSuiteService;
  private final CodingWorker codingWorker;
  private final SolutionEvaluator solutionEvaluator;

  public SolvePlaygroundController(
      TestSuiteService testSuiteService, CodingWorker codingWorker, SolutionEvaluator solutionEvaluator) {
    this.testSuiteService = testSuiteService;
    this.codingWorker = codingWorker;
    this.solutionEvaluator = solutionEvaluator;
  }

  @PostMapping("/solve")
  public SolveResponse solve(@Valid @RequestBody SolveRequest request) {
    VerifiedTestSuite suite = testSuiteService.prepare(request.problem());
    GeneratedSolution solution = codingWorker.generate(new CodeGenerationRequest(
        request.problem(), request.language(), suite.ioSpecification(), null));
    EvaluationResult evaluation = solutionEvaluator.evaluate(request.language(), solution.sourceCode(), suite);
    return new SolveResponse(suite.ioSpecification(), suite.testCases().size(), suite.droppedTests(),
        solution, evaluation);
  }

  public record SolveRequest(@NotBlank @Size(max = 20_000) String problem, @NotNull Language language) {
  }

  public record SolveResponse(
      String ioSpecification,
      int verifiedTests,
      int droppedTests,
      GeneratedSolution solution,
      EvaluationResult evaluation) {
  }
}
