package io.github.tiennnk.problemsolving.protocol.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.tiennnk.problemsolving.evaluation.domain.Evaluation;
import io.github.tiennnk.problemsolving.evaluation.domain.EvaluationResult;
import io.github.tiennnk.problemsolving.evaluation.domain.TestCase;
import io.github.tiennnk.problemsolving.evaluation.domain.TestOutcome;
import io.github.tiennnk.problemsolving.evaluation.domain.VerifiedTestSuite;
import io.github.tiennnk.problemsolving.evaluation.infrastructure.EvaluationRepository;
import io.github.tiennnk.problemsolving.evaluation.infrastructure.TestCaseRepository;
import io.github.tiennnk.problemsolving.execution.domain.Execution;
import io.github.tiennnk.problemsolving.execution.infrastructure.ExecutionRepository;
import io.github.tiennnk.problemsolving.protocol.domain.ProblemSolverState;
import io.github.tiennnk.problemsolving.protocol.domain.ProtocolRun;
import io.github.tiennnk.problemsolving.protocol.infrastructure.ProtocolRunRepository;
import io.github.tiennnk.problemsolving.solution.domain.Solution;
import io.github.tiennnk.problemsolving.solution.infrastructure.SolutionRepository;
import io.github.tiennnk.problemsolving.task.application.TaskService;
import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;
import io.github.tiennnk.problemsolving.worker.domain.WorkerRun;
import io.github.tiennnk.problemsolving.worker.domain.WorkerRunStatus;
import io.github.tiennnk.problemsolving.worker.infrastructure.WorkerRunRepository;

@Service
public class ProblemSolverSteps {

  private static final int MAX_DETAIL_LENGTH = 2_000;

  private final TaskService taskService;
  private final ProtocolRunRepository protocolRunRepository;
  private final TestCaseRepository testCaseRepository;
  private final WorkerRunRepository workerRunRepository;
  private final SolutionRepository solutionRepository;
  private final ExecutionRepository executionRepository;
  private final EvaluationRepository evaluationRepository;

  public ProblemSolverSteps(
      TaskService taskService,
      ProtocolRunRepository protocolRunRepository,
      TestCaseRepository testCaseRepository,
      WorkerRunRepository workerRunRepository,
      SolutionRepository solutionRepository,
      ExecutionRepository executionRepository,
      EvaluationRepository evaluationRepository) {
    this.taskService = taskService;
    this.protocolRunRepository = protocolRunRepository;
    this.testCaseRepository = testCaseRepository;
    this.workerRunRepository = workerRunRepository;
    this.solutionRepository = solutionRepository;
    this.executionRepository = executionRepository;
    this.evaluationRepository = evaluationRepository;
  }

  @Transactional
  public UUID begin(UUID taskId) {
    Task task = taskService.startTask(taskId);
    return protocolRunRepository.save(ProtocolRun.start(task.getId(), task.getProtocolType())).getId();
  }

  @Transactional
  public List<UUID> saveTestCases(UUID protocolRunId, VerifiedTestSuite suite) {
    ProtocolRun run = protocolRun(protocolRunId);
    List<UUID> testCaseIds = new ArrayList<>();
    for (int i = 0; i < suite.testCases().size(); i++) {
      TestCase testCase = testCaseRepository.save(TestCase.from(run.getTaskId(), i + 1, suite.testCases().get(i)));
      testCaseIds.add(testCase.getId());
    }
    run.transitionTo(ProblemSolverState.PLAN);
    run.transitionTo(ProblemSolverState.DISPATCH);
    run.transitionTo(ProblemSolverState.SOLVE);
    return testCaseIds;
  }

  @Transactional
  public UUID startWorker(UUID protocolRunId, String workerName) {
    protocolRun(protocolRunId);
    return workerRunRepository.save(WorkerRun.start(protocolRunId, workerName)).getId();
  }

  @Transactional
  public UUID saveSolution(UUID protocolRunId, UUID workerRunId, int version, GeneratedSolution generated) {
    workerRun(workerRunId).recordUsage(generated.model(), generated.tokenUsage());
    Solution solution = solutionRepository.save(Solution.from(workerRunId, version, generated));
    protocolRun(protocolRunId).transitionTo(ProblemSolverState.EXECUTE);
    return solution.getId();
  }

  @Transactional
  public void saveEvaluation(UUID protocolRunId, UUID solutionId, List<UUID> testCaseIds, EvaluationResult result) {
    for (TestOutcome outcome : result.outcomes()) {
      UUID testCaseId = testCaseIds.get(outcome.testNumber() - 1);
      executionRepository.save(Execution.from(solutionId, testCaseId, outcome));
    }
    evaluationRepository.save(Evaluation.from(solutionId, result, testCaseIds.size()));
    protocolRun(protocolRunId).transitionTo(ProblemSolverState.EVALUATE);
  }

  @Transactional
  public boolean startRepair(UUID protocolRunId) {
    ProtocolRun run = protocolRun(protocolRunId);
    if (!run.canRepair()) {
      return false;
    }
    run.transitionTo(ProblemSolverState.REPAIR);
    return true;
  }

  @Transactional
  public void complete(UUID protocolRunId, UUID workerRunId) {
    workerRun(workerRunId).finish(WorkerRunStatus.SUCCEEDED);
    ProtocolRun run = protocolRun(protocolRunId);
    run.transitionTo(ProblemSolverState.OPTIMIZE);
    run.transitionTo(ProblemSolverState.REPORT);
    run.transitionTo(ProblemSolverState.DONE);
    taskService.completeTask(run.getTaskId());
  }

  @Transactional
  public void fail(UUID protocolRunId, UUID workerRunId, FailureReason reason, String detail) {
    if (workerRunId != null) {
      WorkerRun workerRun = workerRun(workerRunId);
      if (workerRun.getStatus() == WorkerRunStatus.RUNNING) {
        workerRun.finish(WorkerRunStatus.FAILED);
      }
    }
    ProtocolRun run = protocolRun(protocolRunId);
    if (!run.getCurrentState().isTerminal()) {
      run.fail(abbreviate(reason + ": " + detail));
    }
    taskService.failTask(run.getTaskId(), reason);
  }

  private ProtocolRun protocolRun(UUID id) {
    return protocolRunRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Protocol run not found: " + id));
  }

  private WorkerRun workerRun(UUID id) {
    return workerRunRepository.findById(id)
        .orElseThrow(() -> new IllegalStateException("Worker run not found: " + id));
  }

  private static String abbreviate(String text) {
    return text.length() <= MAX_DETAIL_LENGTH ? text : text.substring(0, MAX_DETAIL_LENGTH);
  }
}
