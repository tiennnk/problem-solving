package io.github.tiennnk.problemsolving.common.llm;

public record TokenUsage(int promptTokens, int completionTokens) {

  public static final TokenUsage UNKNOWN = new TokenUsage(0, 0);

  public int totalTokens() {
    return promptTokens + completionTokens;
  }
}
