package io.github.tiennnk.problemsolving.common.llm;

public record LlmResponse(String content, String model, TokenUsage usage) {
}
