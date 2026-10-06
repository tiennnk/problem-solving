package io.github.tiennnk.problemsolving.worker.domain;

import io.github.tiennnk.problemsolving.common.domain.Language;

public record CodeGenerationRequest(
    String problem,
    Language language,
    String ioSpecification,
    String repairFeedback) {

  public CodeGenerationRequest {
    if (problem == null || problem.isBlank()) {
      throw new IllegalArgumentException("problem must not be blank");
    }
    if (language == null) {
      throw new IllegalArgumentException("language must not be null");
    }
  }

  public static CodeGenerationRequest of(String problem, Language language) {
    return new CodeGenerationRequest(problem, language, null, null);
  }
}
