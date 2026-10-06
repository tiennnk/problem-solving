package io.github.tiennnk.problemsolving.protocol.application;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;
import io.github.tiennnk.problemsolving.common.llm.InvalidLlmOutputException;
import io.github.tiennnk.problemsolving.common.llm.LlmException;
import io.github.tiennnk.problemsolving.evaluation.application.SolutionEvaluator;
import io.github.tiennnk.problemsolving.evaluation.application.TestSuitePreparationException;
import io.github.tiennnk.problemsolving.evaluation.application.TestSuiteService;
import io.github.tiennnk.problemsolving.evaluation.domain.EvaluationResult;
import io.github.tiennnk.problemsolving.evaluation.domain.RepairFeedback;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import io.github.tiennnk.problemsolving.task.application.TaskService;
import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.worker.application.CodingWorker;
import io.github.tiennnk.problemsolving.worker.domain.CodeGenerationRequest;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;

@Component
public class ProblemSolverProtocol implements Protocol {

  static final String WORKER_NAME = "llm-coding-worker";

  private static final Logger log = LoggerFactory.getLogger(ProblemSolverProtocol.class);

  private final TaskService taskService;
  private final ProblemSolverSteps steps;
  private final TestSuiteService testSuiteService;
  private final CodingWorker codingWorker;
  private final SolutionEvaluator solutionEvaluator;

  public ProblemSolverProtocol(
      TaskService taskService,
      ProblemSolverSteps steps,
      TestSuiteService testSuiteService,
      CodingWorker codingWorker,
      SolutionEvaluator solutionEvaluator) {
    this.taskService = taskService;
    this.steps = steps;
    this.testSuiteService = testSuiteService;
    this.codingWorker = codingWorker;
    this.solutionEvaluator = solutionEvaluator;
  }

  @Override
  public ProtocolType type() {
    return ProtocolType.PROBLEM_SOLVER;
  }

  @Override
  public void run(UUID taskId) {
    UUID protocolRunId = steps.begin(taskId);
    UUID workerRunId = null;
    try {
      Task task = taskService.getTask(taskId);

      VerifiedTestSuite suite;
      try {
        suite = testSuiteService.prepare(task.getProblem());
      } catch (LlmException | TestSuitePreparationException ex) {
        fail(protocolRunId, null, FailureReason.ANALYZER_ERROR, ex);
        return;
      }
      List<UUID> testCaseIds = steps.saveTestCases(protocolRunId, suite);

      workerRunId = steps.startWorker(protocolRunId, WORKER_NAME);
      String feedback = null;
      for (int version = 1; ; version++) {
        GeneratedSolution generated = codingWorker.generate(new CodeGenerationRequest(
            task.getProblem(), task.getLanguage(), suite.ioSpecification(), feedback));
        UUID solutionId = steps.saveSolution(protocolRunId, workerRunId, version, generated);

        EvaluationResult result = solutionEvaluator.evaluate(task.getLanguage(), generated.sourceCode(), suite);
        steps.saveEvaluation(protocolRunId, solutionId, testCaseIds, result);
        log.info("Task {} solution v{}: {} ({}/{} tests passed)",
            taskId, version, result.verdict(), result.passedTests(), result.totalTests());

        if (result.passed()) {
          steps.complete(protocolRunId, workerRunId);
          return;
        }
        if (!steps.startRepair(protocolRunId)) {
          steps.fail(protocolRunId, workerRunId, FailureReason.REPAIR_EXHAUSTED, result.reason());
          return;
        }
        feedback = RepairFeedback.from(result);
      }
    } catch (InvalidLlmOutputException ex) {
      fail(protocolRunId, workerRunId, FailureReason.NO_PASSING_CANDIDATE, ex);
    } catch (RuntimeException ex) {
      fail(protocolRunId, workerRunId, FailureReason.INFRASTRUCTURE_ERROR, ex);
    }
  }

  private void fail(UUID protocolRunId, UUID workerRunId, FailureReason reason, Exception cause) {
    log.warn("Task protocol run {} failed with {}: {}", protocolRunId, reason, cause.getMessage());
    steps.fail(protocolRunId, workerRunId, reason, String.valueOf(cause.getMessage()));
  }
}
