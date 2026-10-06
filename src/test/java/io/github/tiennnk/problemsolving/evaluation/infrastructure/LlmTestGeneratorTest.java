package io.github.tiennnk.problemsolving.evaluation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.github.tiennnk.problemsolving.common.llm.InvalidLlmOutputException;
import io.github.tiennnk.problemsolving.common.llm.LlmResponse;
import io.github.tiennnk.problemsolving.common.llm.TokenUsage;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestSuite;
import io.github.tiennnk.problemsolving.evaluation.domain.TestCaseCategory;

class LlmTestGeneratorTest {

  private static final String REFERENCE = "public class Main { public static void main(String[] a) { } }";

  private static LlmTestGenerator generatorReturning(String content) {
    return new LlmTestGenerator(request -> new LlmResponse(content, "test-model", new TokenUsage(200, 400)));
  }

  private static String test(String category, String input, String output) {
    return """
        {"category": "%s", "description": "d", "input": "%s", "expectedOutput": "%s"}""".formatted(category, input, output);
  }

  private static String suite(String reference, String... tests) {
    return """
        {"inputFormat": "n, then n integers, then target", "outputFormat": "two indices ascending",
         "referenceSolution": "%s", "tests": [%s]}""".formatted(reference, String.join(",", tests));
  }

  private static String[] fiveValidTests() {
    return new String[] {
        test("BASIC", "4\\n2 7 11 15\\n9", "0 1"),
        test("BASIC", "3\\n3 2 4\\n6", "1 2"),
        test("EDGE", "2\\n3 3\\n6", "0 1"),
        test("EDGE", "4\\n-1 -2 -3 -4\\n-7", "2 3"),
        test("BOUNDARY", "2\\n1000000000 -1000000000\\n0", "0 1")};
  }

  @Test
  void parsesValidSuite() {
    GeneratedTestSuite result = generatorReturning(suite(REFERENCE, fiveValidTests())).generate("Two Sum");

    assertThat(result.testCases()).hasSize(5);
    assertThat(result.testCases().getFirst().input()).isEqualTo("4\n2 7 11 15\n9\n");
    assertThat(result.testCases().getFirst().claimedOutput()).isEqualTo("0 1");
    assertThat(result.testCases().getFirst().category()).isEqualTo(TestCaseCategory.BASIC);
    assertThat(result.referenceSolution()).contains("class Main");
  }

  @Test
  void rejectsMissingReferenceSolution() {
    assertThatThrownBy(() -> generatorReturning(suite("", fiveValidTests())).generate("Two Sum"))
        .isInstanceOf(InvalidLlmOutputException.class)
        .hasMessageContaining("referenceSolution");
  }

  @Test
  void rejectsSuiteMissingACategory() {
    LlmTestGenerator generator = generatorReturning(suite(REFERENCE,
        test("BASIC", "4\\n2 7 11 15\\n9", "0 1"),
        test("BASIC", "3\\n3 2 4\\n6", "1 2"),
        test("BASIC", "2\\n3 3\\n6", "0 1"),
        test("EDGE", "4\\n-1 -2 -3 -4\\n-7", "2 3"),
        test("EDGE", "2\\n0 0\\n0", "0 1")));

    assertThatThrownBy(() -> generator.generate("Two Sum"))
        .isInstanceOf(InvalidLlmOutputException.class)
        .hasMessageContaining("all categories");
  }
}
