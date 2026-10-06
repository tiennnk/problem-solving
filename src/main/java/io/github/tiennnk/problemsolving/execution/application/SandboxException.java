package io.github.tiennnk.problemsolving.execution.application;

public class SandboxException extends RuntimeException {

  public SandboxException(String message) {
    super(message);
  }

  public SandboxException(String message, Throwable cause) {
    super(message, cause);
  }
}
