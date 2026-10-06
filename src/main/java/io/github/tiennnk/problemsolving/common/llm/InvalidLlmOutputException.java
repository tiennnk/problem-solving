package io.github.tiennnk.problemsolving.common.llm;

public class InvalidLlmOutputException extends LlmException {

  public InvalidLlmOutputException(String message) {
    super(message);
  }

  public InvalidLlmOutputException(String message, Throwable cause) {
    super(message, cause);
  }
}
