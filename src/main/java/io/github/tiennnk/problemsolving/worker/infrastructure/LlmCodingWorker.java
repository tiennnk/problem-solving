package io.github.tiennnk.problemsolving.worker.infrastructure;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.llm.InvalidLlmOutputException;
import io.github.tiennnk.problemsolving.common.llm.LlmClient;
import io.github.tiennnk.problemsolving.common.llm.LlmRequest;
import io.github.tiennnk.problemsolving.common.llm.LlmResponse;
import io.github.tiennnk.problemsolving.worker.application.CodingWorker;
import io.github.tiennnk.problemsolving.worker.domain.CodeGenerationRequest;
import io.github.tiennnk.problemsolving.worker.domain.GeneratedSolution;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LlmCodingWorker implements CodingWorker {

  private static final int MAX_SOURCE_LENGTH = 50_000;
  private static final Pattern CODE_FENCE = Pattern.compile("^```[a-zA-Z]*\\s*\\n(.*?)\\n?```\\s*$", Pattern.DOTALL);
  private static final Pattern JAVA_PACKAGE = Pattern.compile("^\\s*package\\s+[\\w.]+\\s*;", Pattern.MULTILINE);

  private static final String JSON_CONTRACT = """
      Respond with a single JSON object and nothing else. It must have exactly these fields:
      - "sourceCode": the complete program as a string
      - "timeComplexity": big-O time complexity, e.g. "O(n log n)"
      - "spaceComplexity": big-O extra space complexity, e.g. "O(n)"
      - "explanation": the idea of the solution in at most 3 sentences
      """;

  private static final String JAVA_RULES = """
      You are an expert competitive programmer. Write a correct and efficient solution in Java 21.
      Program rules:
      - One file containing `public class Main` with `public static void main(String[] args)`.
      - No package declaration. Use only the Java standard library.
      - Read all input from standard input and print only the answer to standard output.
      - Never print prompts, labels or debug output.
      """;

  private static final String PHP_RULES = """
      You are an expert competitive programmer. Write a correct and efficient solution in PHP 8.
      Program rules:
      - One script that starts with `<?php`. Use only the PHP standard library.
      - Read all input from STDIN (for example with stream_get_contents(STDIN)) and print only the answer with echo.
      - Never print prompts, labels or debug output.
      """;

  private static final String DEFAULT_IO_SPECIFICATION = """
      Not specified. Use the most natural plain-text format: values separated by spaces or newlines, \
      an array as a line with its length followed by a line with its elements. \
      Describe the chosen format in the explanation.""";

  private final LlmClient llmClient;
  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  public LlmCodingWorker(LlmClient llmClient) {
    this.llmClient = llmClient;
  }

  @Override
  public GeneratedSolution generate(CodeGenerationRequest request) {
    LlmResponse response = llmClient.complete(LlmRequest.json(systemPrompt(request.language()), userPrompt(request)));
    SolutionPayload payload = parse(response.content());

    String sourceCode = stripCodeFence(requireText(payload.sourceCode(), "sourceCode"));
    validateSource(sourceCode, request.language());

    return new GeneratedSolution(
        request.language(),
        sourceCode,
        requireText(payload.timeComplexity(), "timeComplexity"),
        requireText(payload.spaceComplexity(), "spaceComplexity"),
        payload.explanation() == null ? "" : payload.explanation().strip(),
        response.model(),
        response.usage());
  }

  private static String systemPrompt(Language language) {
    String rules = switch (language) {
      case JAVA -> JAVA_RULES;
      case PHP -> PHP_RULES;
    };
    return rules + "\n" + JSON_CONTRACT;
  }

  private static String userPrompt(CodeGenerationRequest request) {
    StringBuilder prompt = new StringBuilder()
        .append("Problem:\n").append(request.problem().strip())
        .append("\n\nInput/output format:\n")
        .append(isBlank(request.ioSpecification()) ? DEFAULT_IO_SPECIFICATION : request.ioSpecification().strip());
    if (!isBlank(request.repairFeedback())) {
      prompt.append("\n\nYour previous solution failed. Feedback from real execution:\n")
          .append(request.repairFeedback().strip())
          .append("\nFix the program.");
    }
    return prompt.toString();
  }

  private SolutionPayload parse(String content) {
    try {
      return jsonMapper.readValue(stripCodeFence(content.strip()), SolutionPayload.class);
    } catch (JacksonException ex) {
      throw new InvalidLlmOutputException("Coding worker output is not valid JSON: " + ex.getOriginalMessage(), ex);
    }
  }

  private static void validateSource(String sourceCode, Language language) {
    if (sourceCode.length() > MAX_SOURCE_LENGTH) {
      throw new InvalidLlmOutputException("sourceCode is longer than " + MAX_SOURCE_LENGTH + " characters");
    }
    switch (language) {
      case JAVA -> {
        if (!sourceCode.contains("class Main") || !sourceCode.contains("static void main")) {
          throw new InvalidLlmOutputException("Java solution must declare class Main with a main method");
        }
        if (JAVA_PACKAGE.matcher(sourceCode).find()) {
          throw new InvalidLlmOutputException("Java solution must not declare a package");
        }
      }
      case PHP -> {
        if (!sourceCode.stripLeading().startsWith("<?php")) {
          throw new InvalidLlmOutputException("PHP solution must start with <?php");
        }
      }
    }
  }

  private static String stripCodeFence(String text) {
    var matcher = CODE_FENCE.matcher(text.strip());
    return matcher.matches() ? matcher.group(1).strip() : text.strip();
  }

  private static String requireText(String value, String field) {
    if (isBlank(value)) {
      throw new InvalidLlmOutputException("Coding worker output is missing field: " + field);
    }
    return value.strip();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record SolutionPayload(String sourceCode, String timeComplexity, String spaceComplexity, String explanation) {
  }
}
