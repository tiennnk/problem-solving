package io.github.tiennnk.problemsolving.execution.domain;

public record CompileResult(boolean success, String output, long durationMs) {

  public static CompileResult skipped() {
    return new CompileResult(true, "", 0);
  }
}
