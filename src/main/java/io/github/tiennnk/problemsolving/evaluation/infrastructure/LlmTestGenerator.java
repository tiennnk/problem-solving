package io.github.tiennnk.problemsolving.evaluation.infrastructure;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.github.tiennnk.problemsolving.common.llm.InvalidLlmOutputException;
import io.github.tiennnk.problemsolving.common.llm.LlmClient;
import io.github.tiennnk.problemsolving.common.llm.LlmRequest;
import io.github.tiennnk.problemsolving.common.llm.LlmResponse;
import io.github.tiennnk.problemsolving.evaluation.application.TestGenerator;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestCase;
import io.github.tiennnk.problemsolving.evaluation.domain.GeneratedTestSuite;
import io.github.tiennnk.problemsolving.evaluation.domain.TestCaseCategory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LlmTestGenerator implements TestGenerator {

  static final int MIN_TESTS = 5;
  static final int MAX_TESTS = 20;
  static final int MAX_INPUT_LENGTH = 5_000;

  private static final Pattern CODE_FENCE = Pattern.compile("^```[a-zA-Z]*\\s*\\n(.*?)\\n?```\\s*$", Pattern.DOTALL);

  private static final String SYSTEM_PROMPT = """
      You are a meticulous test designer for algorithm problems. You never see any solution code.
      Your job:
      1. Define a plain-text stdin input format and a stdout output format for the problem.
         - Prefer: numbers separated by spaces; an array as one line with its length followed by one line with its elements.
         - The output format must make the correct answer unique (for example: "indices in ascending order",
           "print -1 if there is no answer", "print true or false in lowercase").
      2. Write 8 to 12 test inputs in three categories:
         - BASIC: typical cases, including the examples from the statement.
         - EDGE: tricky cases such as duplicates, negative numbers, zeros, minimum length, all equal values.
         - BOUNDARY: extreme values allowed by the constraints, but keep every input shorter than 2000 characters
           and small enough for a brute-force solution to finish within one second.
         For each test also give the output you expect. It will be cross-checked by running code.
      3. Write a reference solution in Java 21 that solves the problem with the simplest, most obviously correct
         brute-force method. Correctness matters far more than speed.
         - One file containing `public class Main` with `public static void main(String[] args)`, no package.
         - Read the input exactly as described in inputFormat and print exactly as described in outputFormat.
      Respond with a single JSON object and nothing else:
      {
        "inputFormat": "...",
        "outputFormat": "...",
        "referenceSolution": "...",
        "tests": [
          {"category": "BASIC|EDGE|BOUNDARY", "description": "...", "input": "...", "expectedOutput": "..."}
        ]
      }
      Use "\\n" for line breaks inside strings.
      """;

  private final LlmClient llmClient;
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  public LlmTestGenerator(LlmClient llmClient) {
    this.llmClient = llmClient;
  }

  @Override
  public GeneratedTestSuite generate(String problem) {
    if (problem == null || problem.isBlank()) {
      throw new IllegalArgumentException("problem must not be blank");
    }

    LlmResponse response = llmClient.complete(LlmRequest.json(SYSTEM_PROMPT, "Problem:\n" + problem.strip()));
    SuitePayload payload = parse(response.content());

    List<GeneratedTestCase> testCases = toTestCases(payload.tests());
    return new GeneratedTestSuite(
        requireText(payload.inputFormat(), "inputFormat"),
        requireText(payload.outputFormat(), "outputFormat"),
        validReferenceSolution(payload.referenceSolution()),
        testCases,
        response.model(),
        response.usage());
  }

  private static String validReferenceSolution(String referenceSolution) {
    String source = stripCodeFence(requireText(referenceSolution, "referenceSolution"));
    if (!source.contains("class Main") || !source.contains("static void main")) {
      throw new InvalidLlmOutputException("Reference solution must declare class Main with a main method");
    }
    return source;
  }

  private SuitePayload parse(String content) {
    try {
      return jsonMapper.readValue(stripCodeFence(content), SuitePayload.class);
    } catch (JacksonException ex) {
      throw new InvalidLlmOutputException("Test generator output is not valid JSON: " + ex.getOriginalMessage(), ex);
    }
  }

  private static List<GeneratedTestCase> toTestCases(List<TestPayload> tests) {
    if (tests == null) {
      throw new InvalidLlmOutputException("Test generator output is missing field: tests");
    }

    List<GeneratedTestCase> testCases = new ArrayList<>();
    Set<String> seenInputs = new HashSet<>();
    Set<TestCaseCategory> categories = EnumSet.noneOf(TestCaseCategory.class);
    for (TestPayload test : tests) {
      if (test == null) {
        continue;
      }
      TestCaseCategory category = parseCategory(test.category());
      String input = normalizeNewlines(test.input() == null ? "" : test.input());
      String claimedOutput = normalizeNewlines(requireText(test.expectedOutput(), "expectedOutput"));
      if (input.length() > MAX_INPUT_LENGTH) {
        throw new InvalidLlmOutputException("Test input is longer than " + MAX_INPUT_LENGTH + " characters");
      }
      if (!seenInputs.add(input.strip())) {
        continue;
      }
      categories.add(category);
      String description = test.description() == null ? "" : test.description().strip();
      testCases.add(new GeneratedTestCase(category, description, ensureTrailingNewline(input), claimedOutput));
    }

    if (testCases.size() < MIN_TESTS || testCases.size() > MAX_TESTS) {
      throw new InvalidLlmOutputException("Expected %d to %d distinct tests but got %d"
          .formatted(MIN_TESTS, MAX_TESTS, testCases.size()));
    }
    if (!categories.containsAll(EnumSet.allOf(TestCaseCategory.class))) {
      throw new InvalidLlmOutputException("Tests must cover all categories, got " + categories);
    }
    return testCases;
  }

  private static TestCaseCategory parseCategory(String value) {
    try {
      return TestCaseCategory.valueOf(requireText(value, "category").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new InvalidLlmOutputException("Unknown test category: " + value, ex);
    }
  }

  private static String stripCodeFence(String text) {
    Matcher matcher = CODE_FENCE.matcher(text.strip());
    return matcher.matches() ? matcher.group(1).strip() : text.strip();
  }

  private static String normalizeNewlines(String text) {
    return text.replace("\r\n", "\n");
  }

  private static String ensureTrailingNewline(String input) {
    return input.isEmpty() || input.endsWith("\n") ? input : input + "\n";
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new InvalidLlmOutputException("Test generator output is missing field: " + field);
    }
    return value.strip();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record SuitePayload(String inputFormat, String outputFormat, String referenceSolution, List<TestPayload> tests) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TestPayload(String category, String description, String input, String expectedOutput) {
  }
}
