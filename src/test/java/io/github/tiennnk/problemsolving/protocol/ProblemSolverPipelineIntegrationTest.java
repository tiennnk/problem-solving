package io.github.tiennnk.problemsolving.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import io.github.tiennnk.problemsolving.common.llm.LlmClient;
import io.github.tiennnk.problemsolving.common.llm.LlmResponse;
import io.github.tiennnk.problemsolving.common.llm.TokenUsage;
import io.github.tiennnk.problemsolving.evaluation.domain.Evaluation;
import io.github.tiennnk.problemsolving.evaluation.domain.Verdict;
import io.github.tiennnk.problemsolving.evaluation.infrastructure.EvaluationRepository;
import io.github.tiennnk.problemsolving.evaluation.infrastructure.TestCaseRepository;
import io.github.tiennnk.problemsolving.execution.domain.Execution;
import io.github.tiennnk.problemsolving.execution.infrastructure.ExecutionRepository;
import io.github.tiennnk.problemsolving.protocol.domain.ProblemSolverState;
import io.github.tiennnk.problemsolving.protocol.domain.ProtocolRun;
import io.github.tiennnk.problemsolving.protocol.infrastructure.ProtocolRunRepository;
import io.github.tiennnk.problemsolving.solution.domain.Solution;
import io.github.tiennnk.problemsolving.solution.infrastructure.SolutionRepository;
import io.github.tiennnk.problemsolving.support.PostgresTestcontainersConfiguration;
import io.github.tiennnk.problemsolving.task.application.TaskService;
import io.github.tiennnk.problemsolving.task.domain.FailureReason;
import io.github.tiennnk.problemsolving.task.domain.Task;
import io.github.tiennnk.problemsolving.task.domain.TaskStatus;
import io.github.tiennnk.problemsolving.worker.domain.WorkerRun;
import io.github.tiennnk.problemsolving.worker.infrastructure.WorkerRunRepository;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestcontainersConfiguration.class, ProblemSolverPipelineIntegrationTest.ScriptedLlm.class})
class ProblemSolverPipelineIntegrationTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final String REFERENCE = """
      import java.util.*;
      public class Main {
        public static void main(String[] args) {
          Scanner in = new Scanner(System.in);
          int n = in.nextInt();
          long[] a = new long[n];
          for (int i = 0; i < n; i++) a[i] = in.nextLong();
          long t = in.nextLong();
          for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
            if (a[i] + a[j] == t) { System.out.println(i + " " + j); return; }
        }
      }
      """;

  private static final String CORRECT = """
      import java.util.*;
      public class Main {
        public static void main(String[] args) {
          Scanner in = new Scanner(System.in);
          int n = in.nextInt();
          long[] a = new long[n];
          for (int i = 0; i < n; i++) a[i] = in.nextLong();
          long t = in.nextLong();
          Map<Long, Integer> seen = new HashMap<>();
          for (int j = 0; j < n; j++) {
            Integer i = seen.get(t - a[j]);
            if (i != null) { System.out.println(i + " " + j); return; }
            seen.put(a[j], j);
          }
        }
      }
      """;

  private static final String WRONG = CORRECT.replace("i + \" \" + j", "j + \" \" + i");

  static final AtomicInteger codingCalls = new AtomicInteger();
  static volatile boolean workerAlwaysWrong;

  @TestConfiguration
  static class ScriptedLlm {

    @Bean
    @Primary
    LlmClient scriptedLlmClient() {
      return request -> {
        if (request.systemPrompt().contains("test designer")) {
          return response(testSuite());
        }
        int call = codingCalls.incrementAndGet();
        String source = workerAlwaysWrong || call == 1 ? WRONG : CORRECT;
        return response(JSON.writeValueAsString(Map.of(
            "sourceCode", source, "timeComplexity", "O(n)", "spaceComplexity", "O(n)",
            "explanation", "Hash map of seen values.")));
      };
    }

    private static LlmResponse response(String content) {
      return new LlmResponse(content, "scripted-model", new TokenUsage(100, 50));
    }

    private static String testSuite() {
      return JSON.writeValueAsString(Map.of(
          "inputFormat", "n, then n integers, then target",
          "outputFormat", "two indices in ascending order",
          "referenceSolution", REFERENCE,
          "tests", List.of(
              test("BASIC", "4\n2 7 11 15\n9", "0 1"),
              test("BASIC", "3\n3 2 4\n6", "1 2"),
              test("EDGE", "2\n3 3\n6", "0 1"),
              test("EDGE", "4\n-1 -2 -3 -4\n-7", "2 3"),
              test("BOUNDARY", "2\n1000000000 -1000000000\n0", "0 1"))));
    }

    private static Map<String, String> test(String category, String input, String output) {
      return Map.of("category", category, "description", category.toLowerCase() + " case",
          "input", input, "expectedOutput", output);
    }
  }

  @Autowired
  MockMvc mockMvc;

  @Autowired
  TaskService taskService;

  @Autowired
  ProtocolRunRepository protocolRunRepository;

  @Autowired
  TestCaseRepository testCaseRepository;

  @Autowired
  WorkerRunRepository workerRunRepository;

  @Autowired
  SolutionRepository solutionRepository;

  @Autowired
  EvaluationRepository evaluationRepository;

  @Autowired
  ExecutionRepository executionRepository;

  @BeforeEach
  void resetScript() {
    codingCalls.set(0);
    workerAlwaysWrong = false;
  }

  @Test
  void twoSumIsRepairedOnceThenCompletesWithAPassingSolution() throws Exception {
    UUID taskId = submitTwoSum();
    assertThat(taskService.getTask(taskId).getStatus()).isIn(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

    Task task = awaitTerminal(taskId);

    assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    ProtocolRun run = protocolRunRepository.findByTaskIdOrderByStartedAtDesc(taskId).getFirst();
    assertThat(run.getCurrentState()).isEqualTo(ProblemSolverState.DONE);
    assertThat(run.getAttempt()).isEqualTo(1);
    assertThat(testCaseRepository.findByTaskIdOrderByPosition(taskId)).hasSize(5);

    List<Solution> solutions = solutionsOf(run);
    assertThat(solutions).extracting(Solution::getVersion).containsExactly(1, 2);
    assertThat(evaluationOf(solutions.get(0)).getStatus()).isEqualTo(Verdict.WRONG_ANSWER);
    Evaluation finalEvaluation = evaluationOf(solutions.get(1));
    assertThat(finalEvaluation.getStatus()).isEqualTo(Verdict.PASS);
    assertThat(finalEvaluation.getPassedTests()).isEqualTo(5);
    assertThat(executionRepository.findBySolutionId(solutions.get(1).getId()))
        .hasSize(5)
        .extracting(Execution::getStatus)
        .containsOnly(Verdict.PASS);

    WorkerRun workerRun = workerRunRepository.findByProtocolRunId(run.getId()).getFirst();
    assertThat(workerRun.getPromptTokens()).isEqualTo(200);
  }

  @Test
  void failsWithRepairExhaustedWhenEveryVersionIsWrong() throws Exception {
    workerAlwaysWrong = true;

    Task task = awaitTerminal(submitTwoSum());

    assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
    assertThat(task.getFailureReason()).isEqualTo(FailureReason.REPAIR_EXHAUSTED);
    ProtocolRun run = protocolRunRepository.findByTaskIdOrderByStartedAtDesc(task.getId()).getFirst();
    assertThat(run.getCurrentState()).isEqualTo(ProblemSolverState.FAILED);
    assertThat(solutionsOf(run)).extracting(Solution::getVersion).containsExactly(1, 2, 3);
  }

  private UUID submitTwoSum() throws Exception {
    String body = mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"problem": "Two Sum: return indices of the two numbers that add up to target.", "language": "JAVA"}
                """))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  private Task awaitTerminal(UUID taskId) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofMinutes(3).toNanos();
    while (System.nanoTime() < deadline) {
      Task task = taskService.getTask(taskId);
      if (task.getStatus().isFinal()) {
        return task;
      }
      Thread.sleep(500);
    }
    throw new AssertionError("Task " + taskId + " did not finish in time");
  }

  private List<Solution> solutionsOf(ProtocolRun run) {
    List<UUID> workerRunIds = workerRunRepository.findByProtocolRunId(run.getId()).stream()
        .map(WorkerRun::getId).toList();
    return solutionRepository.findByWorkerRunIdInOrderByCreatedAt(workerRunIds).stream()
        .sorted(Comparator.comparingInt(Solution::getVersion))
        .toList();
  }

  private Evaluation evaluationOf(Solution solution) {
    return evaluationRepository.findBySolutionId(solution.getId()).orElseThrow();
  }
}
