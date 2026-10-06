package io.github.tiennnk.problemsolving.worker.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.llm.InvalidLlmOutputException;
import io.github.tiennnk.problemsolving.common.llm.LlmResponse;
import io.github.tiennnk.problemsolving.common.llm.TokenUsage;
import io.github.tiennnk.problemsolving.worker.domain.CodeGenerationRequest;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;

class LlmCodingWorkerTest {

  private static final String JAVA_SOURCE =
      "import java.util.*;\\npublic class Main { public static void main(String[] args) { System.out.println(1); } }";

  private static LlmCodingWorker workerReturning(String content) {
    return new LlmCodingWorker(request -> new LlmResponse(content, "test-model", new TokenUsage(100, 50)));
  }

  @Test
  void parsesValidJavaSolution() {
    LlmCodingWorker worker = workerReturning("""
        {"sourceCode": "%s", "timeComplexity": "O(n)", "spaceComplexity": "O(n)", "explanation": "Use a hash map."}
        """.formatted(JAVA_SOURCE));

    GeneratedSolution solution = worker.generate(CodeGenerationRequest.of("Two Sum", Language.JAVA));

    assertThat(solution.sourceCode()).startsWith("import java.util.*;").contains("public class Main");
    assertThat(solution.claimedTimeComplexity()).isEqualTo("O(n)");
    assertThat(solution.tokenUsage().totalTokens()).isEqualTo(150);
  }

  @Test
  void acceptsJsonWrappedInMarkdownFence() {
    LlmCodingWorker worker = workerReturning("""
        ```json
        {"sourceCode": "%s", "timeComplexity": "O(n)", "spaceComplexity": "O(1)"}
        ```
        """.formatted(JAVA_SOURCE));

    assertThat(worker.generate(CodeGenerationRequest.of("Two Sum", Language.JAVA)).sourceCode())
        .contains("class Main");
  }

  @Test
  void rejectsNonJsonOutput() {
    LlmCodingWorker worker = workerReturning("Here is my solution: public class Main {}");

    assertThatThrownBy(() -> worker.generate(CodeGenerationRequest.of("Two Sum", Language.JAVA)))
        .isInstanceOf(InvalidLlmOutputException.class)
        .hasMessageContaining("not valid JSON");
  }

  @Test
  void rejectsJavaWithoutMainClass() {
    LlmCodingWorker worker = workerReturning("""
        {"sourceCode": "class Solution { int[] twoSum(int[] a, int t) { return a; } }", "timeComplexity": "O(n)", "spaceComplexity": "O(1)"}
        """);

    assertThatThrownBy(() -> worker.generate(CodeGenerationRequest.of("Two Sum", Language.JAVA)))
        .isInstanceOf(InvalidLlmOutputException.class)
        .hasMessageContaining("class Main");
  }
}
