package io.github.tiennnk.problemsolving.common.llm;

public record LlmRequest(String systemPrompt, String userPrompt, boolean jsonOutput) {

  public LlmRequest {
    if (userPrompt == null || userPrompt.isBlank()) {
      throw new IllegalArgumentException("userPrompt must not be blank");
    }
  }

  public static LlmRequest text(String systemPrompt, String userPrompt) {
    return new LlmRequest(systemPrompt, userPrompt, false);
  }

  public static LlmRequest json(String systemPrompt, String userPrompt) {
    return new LlmRequest(systemPrompt, userPrompt, true);
  }
}
