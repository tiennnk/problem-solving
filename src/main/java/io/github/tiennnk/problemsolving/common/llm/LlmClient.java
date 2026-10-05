package io.github.tiennnk.problemsolving.common.llm;

public interface LlmClient {

  LlmResponse complete(LlmRequest request);
}
